(function () {
  'use strict';

  // ---------- Thème ----------
  // Choix mémorisé dans le navigateur (localStorage) ; sans choix, on suit le thème de l'appareil.
  var racine = document.documentElement;
  var bouton = document.querySelector('[data-theme-toggle]');
  var sombreAppareil = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;

  function lireChoix() {
    try {
      var t = localStorage.getItem('theme');
      return (t === 'light' || t === 'dark') ? t : null;
    } catch (e) { return null; }
  }
  function themeActuel() {
    return lireChoix() || (sombreAppareil && sombreAppareil.matches ? 'dark' : 'light');
  }
  function appliquer(t) {
    racine.setAttribute('data-theme', t);
    if (bouton) {
      bouton.textContent = t === 'dark' ? 'Mode clair' : 'Mode sombre';
      bouton.setAttribute('aria-label', t === 'dark' ? 'Passer au thème clair' : 'Passer au thème sombre');
    }
  }
  appliquer(themeActuel());

  if (bouton) {
    bouton.addEventListener('click', function () {
      var suivant = racine.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
      try { localStorage.setItem('theme', suivant); } catch (e) { /* stockage indisponible : le choix vaut pour cette page */ }
      appliquer(suivant);
    });
  }
  // Sans choix enregistré, on suit les changements de thème de l'appareil
  if (sombreAppareil && sombreAppareil.addEventListener) {
    sombreAppareil.addEventListener('change', function () {
      if (!lireChoix()) appliquer(themeActuel());
    });
  }

  // ---------- Menu mobile ----------
  var burger = document.querySelector('.burger');
  var nav = document.getElementById('nav');
  if (burger && nav) {
    burger.addEventListener('click', function () {
      var ouvert = nav.classList.toggle('ouvert');
      burger.setAttribute('aria-expanded', ouvert ? 'true' : 'false');
    });
  }

  // ---------- Calculateur (même logique que CalculateurEntretien.java) ----------
  var form = document.getElementById('simulateur');
  if (form) {
    var sortie = document.getElementById('simu-resultat');
    form.addEventListener('submit', function (e) { e.preventDefault(); });
    var calc = function () {
      var kmActuel = parseInt(form.kmActuel.value, 10);
      var kmDernier = parseInt(form.kmDernier.value, 10);
      var intervKm = parseInt(form.intervKm.value, 10);
      var mois = parseInt(form.mois.value, 10);
      var intervMois = parseInt(form.intervMois.value, 10);
      if ([kmActuel, kmDernier, intervKm, mois, intervMois].some(isNaN) || intervKm <= 0 || intervMois <= 0) {
        sortie.textContent = 'Remplissez tous les champs avec des nombres (intervalles supérieurs à 0).';
        sortie.className = 'bandeau';
        return;
      }
      var usure = Math.max((kmActuel - kmDernier) / intervKm * 100, mois / intervMois * 100);
      var niveau = usure >= 100 ? 'urgent' : usure >= 80 ? 'prevoir' : 'ok';
      var libelle = { urgent: 'Urgent : à faire maintenant', prevoir: 'À prévoir bientôt', ok: 'OK : rien à faire' }[niveau];
      sortie.textContent = libelle + ' (usure : ' + Math.round(usure) + ' %)';
      sortie.className = 'bandeau simu-' + niveau;
    };
    // Seuls les chiffres sont acceptés (saisie, collage, glisser-déposer)
    form.addEventListener('input', function (e) {
      var champ = e.target;
      if (champ && typeof champ.value === 'string') {
        var propre = champ.value.replace(/\D/g, '');
        if (propre !== champ.value) champ.value = propre;
      }
      calc();
    });
    calc();
  }
})();
