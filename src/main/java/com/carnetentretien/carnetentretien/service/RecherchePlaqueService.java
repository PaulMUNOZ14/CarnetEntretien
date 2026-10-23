package com.carnetentretien.carnetentretien.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recherche d'un véhicule à partir de sa plaque d'immatriculation, via apiplaqueimmatriculation.com.
 *
 * IMPORTANT : {@link #rechercher(String)} fait un appel réseau (jusqu'à ~10 s). Il ne doit JAMAIS être
 * appelé directement depuis le thread de l'interface JavaFX (la fenêtre se figerait) : utiliser un Task.
 *
 * Clé d'API (dans cet ordre) :
 *   1. variable d'environnement PLAQUE_API_TOKEN
 *   2. fichier config.properties (dossier de lancement) : plaque.api.token=VOTRE_CLE
 *   3. clé de démonstration de la documentation du service (probablement limitée : à remplacer par votre propre clé)
 */
public class RecherchePlaqueService {

    private static final String URL_PLAQUE = "https://api.apiplaqueimmatriculation.com/plaque";
    private static final String TOKEN_DEMO = "TokenDemo2026B";

    private static final Pattern PLAQUE_SIV = Pattern.compile("^[A-Z]{2}\\d{3}[A-Z]{2}$");        // AB-123-CD (depuis 2009)
    private static final Pattern PLAQUE_FNI = Pattern.compile("^\\d{1,4}[A-Z]{1,3}\\d{2,3}$");   // 1234 AB 56 (avant 2009)

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public record Resultat(boolean trouve, String marque, String modele, String version,
                           String annee, String energie, String imageUrl, String logoUrl,
                           String messageErreur) {
        static Resultat echec(String message) {
            return new Resultat(false, null, null, null, null, null, null, null, message);
        }
    }

    public static String normaliser(String plaque) {
        return plaque == null ? "" : plaque.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    public static boolean formatValide(String plaque) {
        String p = normaliser(plaque);
        return PLAQUE_SIV.matcher(p).matches() || PLAQUE_FNI.matcher(p).matches();
    }

    public static String formater(String plaque) {
        String p = normaliser(plaque);
        if (PLAQUE_SIV.matcher(p).matches()) {
            return p.substring(0, 2) + "-" + p.substring(2, 5) + "-" + p.substring(5);
        }
        return plaque == null ? "" : plaque.trim().toUpperCase();
    }

    public static Resultat rechercher(String plaque) {
        String p = normaliser(plaque);
        if (!formatValide(p)) {
            return Resultat.echec("Cette plaque ne ressemble pas à une plaque française. Exemple : AB-123-CD.");
        }

        String token = lireToken();
        boolean demo = TOKEN_DEMO.equals(token);

        try {
            String url = URL_PLAQUE
                    + "?immatriculation=" + URLEncoder.encode(p, StandardCharsets.UTF_8)
                    + "&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                    + "&pays=FR";

            HttpRequest requete = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> reponse = CLIENT.send(requete, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int statut = reponse.statusCode();

            if (statut == 401 || statut == 403) {
                return Resultat.echec("La clé d'accès au service de recherche est refusée. Vérifiez la clé dans config.properties.");
            }
            if (statut == 429) {
                return Resultat.echec("Trop de recherches ont été faites. Réessayez dans quelques minutes.");
            }
            if (statut != 200) {
                return Resultat.echec("Le service de recherche ne répond pas correctement (code " + statut + "). Réessayez plus tard.");
            }

            Object racine = MiniJson.parse(reponse.body());
            if (!(racine instanceof Map<?, ?> map)) {
                return Resultat.echec("La réponse du service est illisible.");
            }

            Object codeErreur = map.get("code_erreur");
            Object data = map.get("data");
            String messageApi = texte(map.get("message"));

            if (!(data instanceof Map<?, ?> d)) {
                return Resultat.echec(messageNonTrouve(messageApi, demo));
            }

            String erreurData = texte(d.get("erreur"));
            String marque = texte(d.get("marque"));
            String modele = texte(d.get("modele"));

            boolean codeKo = codeErreur instanceof Number n && n.intValue() != 200;
            if (codeKo || !erreurData.isBlank() || marque.isBlank() || modele.isBlank()) {
                return Resultat.echec(messageNonTrouve(!erreurData.isBlank() ? erreurData : messageApi, demo));
            }

            return new Resultat(
                    true,
                    mettreEnForme(marque, true),
                    mettreEnForme(modele, false),
                    texte(d.get("version")),
                    extraireAnnee(texte(d.get("date1erCir_fr"))),
                    mettreEnForme(energie(d), false),
                    urlImage(texte(d.get("photo_modele"))),
                    urlImage(texte(d.get("logo_marque"))),
                    null);

        } catch (java.net.http.HttpTimeoutException e) {
            return Resultat.echec("Le service met trop de temps à répondre. Vérifiez votre connexion Internet, ou saisissez la marque et le modèle vous-même.");
        } catch (IOException e) {
            return Resultat.echec("Impossible de joindre le service. Vérifiez votre connexion Internet, ou saisissez la marque et le modèle vous-même.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Resultat.echec("La recherche a été interrompue.");
        } catch (RuntimeException e) {
            return Resultat.echec("La réponse du service est inattendue. Saisissez la marque et le modèle vous-même.");
        }
    }

    private static String messageNonTrouve(String detailApi, boolean demo) {
        String base = "Aucun véhicule trouvé pour cette plaque.";
        if (demo) {
            base += " (Vous utilisez la clé de démonstration, qui est peut-être limitée. " + "Ajoutez votre propre clé dans config.properties.)";
        }
        if (detailApi != null && !detailApi.isBlank()) {
            base += " Détail du service : " + detailApi;
        }
        return base;
    }

    private static String lireToken() {
        String env = System.getenv("PLAQUE_API_TOKEN");
        if (env != null && !env.isBlank()) return env.trim();

        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("config.properties")) {
            props.load(in);
            String fichier = props.getProperty("plaque.api.token");
            if (fichier != null && !fichier.isBlank()) return fichier.trim();
        } catch (IOException ignore) {
        }
        return TOKEN_DEMO;
    }

    private static String texte(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String urlImage(String s) {
        if (s == null || s.isBlank()) return null;
        if (s.startsWith("//")) s = "https:" + s;
        return (s.startsWith("http://") || s.startsWith("https://")) ? s : null;
    }

    private static String extraireAnnee(String dateFr) {
        Matcher m = Pattern.compile("(19|20)\\d{2}").matcher(dateFr == null ? "" : dateFr);
        return m.find() ? m.group() : null;
    }

    private static final Set<String> SIGLES = Set.of("BMW", "MG", "DS", "BYD", "VW", "GMC", "KTM", "SUV", "GPL", "GTI");

    private static String energie(Map<?, ?> d) {
        String e = texte(d.get("energieNGC"));
        if (e.isBlank()) e = texte(d.get("type_moteur"));
        return e.matches("\\d+") ? "" : e;
    }

    private static String mettreEnForme(String brut, boolean marque) {
        if (brut == null || brut.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        Matcher m = Pattern.compile("[^\\s-]+|[\\s-]+").matcher(brut.trim());   // mots, et séparateurs (espace ou tiret)
        while (m.find()) {
            String mot = m.group();
            if (mot.matches("[\\s-]+")) {
                sb.append(mot);
                continue;
            }
            boolean garder = SIGLES.contains(mot.toUpperCase())
                    || mot.matches(".*\\d.*")
                    || (!marque && mot.length() <= 3 && mot.equals(mot.toUpperCase()) && mot.matches("[IVX]+"));
            if (garder) sb.append(mot.toUpperCase());
            else sb.append(mot.substring(0, 1).toUpperCase()).append(mot.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private static final class MiniJson {
        private final String s;
        private int i = 0;

        private MiniJson(String s) { this.s = s; }

        static Object parse(String json) {
            MiniJson p = new MiniJson(json);
            p.espaces();
            Object v = p.valeur();
            p.espaces();
            return v;
        }

        private Object valeur() {
            espaces();
            if (i >= s.length()) throw new IllegalStateException("JSON incomplet");
            char c = s.charAt(i);
            if (c == '{') return objet();
            if (c == '[') return tableau();
            if (c == '"') return chaine();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            return nombre();
        }

        private Map<String, Object> objet() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++; // {
            espaces();
            if (s.charAt(i) == '}') { i++; return m; }
            while (true) {
                espaces();
                String cle = chaine();
                espaces();
                attendre(':');
                m.put(cle, valeur());
                espaces();
                if (s.charAt(i) == ',') { i++; continue; }
                attendre('}');
                return m;
            }
        }

        private List<Object> tableau() {
            List<Object> l = new ArrayList<>();
            i++; // [
            espaces();
            if (s.charAt(i) == ']') { i++; return l; }
            while (true) {
                l.add(valeur());
                espaces();
                if (s.charAt(i) == ',') { i++; continue; }
                attendre(']');
                return l;
            }
        }

        private String chaine() {
            attendre('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n' -> sb.append('\n');
                        case 't' -> sb.append('\t');
                        case 'r' -> sb.append('\r');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                        default -> sb.append(e);   // \" \\ \/
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        private Number nombre() {
            int debut = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (debut == i) throw new IllegalStateException("JSON invalide");
            return Double.valueOf(s.substring(debut, i));
        }

        private void attendre(char c) {
            if (s.charAt(i) != c) throw new IllegalStateException("JSON invalide : '" + c + "' attendu");
            i++;
        }

        private void espaces() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }
    }
}
