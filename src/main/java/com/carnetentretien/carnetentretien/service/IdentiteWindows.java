package com.carnetentretien.carnetentretien.service;

import com.sun.jna.WString;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.WinReg;

import java.util.Locale;

/**
 * Identité de l'application auprès de Windows, utilisée pour le nom affiché dans les
 * notifications de la zone de notification.
 * <p>
 * Sans identité explicite, Windows reprend le nom du fichier exécutable : lancée par
 * {@code java.exe}, l'application apparaît sous le nom « OpenJDK Platform binary ».
 * L'identifiant {@link #AUMID} est donc enregistré dans le registre de l'utilisateur avec
 * le nom « Carnet d'Entretien », puis associé au processus avant la création de toute
 * fenêtre. Les notifications affichent alors ce nom, aussi bien depuis l'IDE que depuis
 * l'application installée.
 */
public final class IdentiteWindows {

    /** Identifiant d'application Windows (AppUserModelID), stable d'une version à l'autre. */
    static final String AUMID = "PaulMUNOZ14.CarnetEntretien";

    /** Nom affiché par Windows à la place de « OpenJDK Platform binary ». */
    static final String NOM_AFFICHE = "Carnet d'Entretien";

    /** Clé du registre, propre à l'utilisateur, qui associe le nom à l'identifiant. */
    private static final String CLE_AUMID = "Software\\Classes\\AppUserModelId\\" + AUMID;

    private static boolean appliquee;

    private IdentiteWindows() {
    }

    /**
     * Déclare l'identité de l'application et l'associe au processus courant. Sans effet hors
     * de Windows, après le premier appel, ou si Windows refuse l'opération : l'application
     * continue alors de fonctionner avec le nom par défaut de son exécutable.
     * <p>
     * À appeler au tout début du démarrage, avant l'affichage de la fenêtre principale.
     */
    public static synchronized void appliquer() {
        if (appliquee || !estWindows()) return;
        appliquee = true;

        try {
            Advapi32Util.registryCreateKey(WinReg.HKEY_CURRENT_USER, CLE_AUMID);
            Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, CLE_AUMID,
                    "DisplayName", NOM_AFFICHE);

            HRESULT resultat = Shell32.INSTANCE.SetCurrentProcessExplicitAppUserModelID(new WString(AUMID));
            if (resultat.intValue() != 0) {
                System.err.println("Nom de notification Windows non appliqué (code "
                        + resultat.intValue() + ").");
            }
        } catch (RuntimeException | LinkageError e) {
            System.err.println("Nom de notification Windows non appliqué : " + e.getMessage());
        }
    }

    private static boolean estWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
