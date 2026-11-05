<div align="center">
  <img src="website/assets/logo.jpg" alt="Logo MaNette" width="150" style="border-radius:50%;">
  <h1>🎮 MaNette v2.0 Pro</h1>
  <p><b>Transformez votre smartphone Android en une véritable manette de jeu Bluetooth HID 60Hz pour votre PC.</b></p>
  <p><i>Conçu et développé par <b>MaxAdis</b></i></p>

  <img src="website/assets/manette_v2.jpg" alt="Interface MaNette v2.0" width="680" style="border-radius:18px; margin: 20px 0;">

  <div>
    <img src="https://img.shields.io/badge/Version-v2.0_Pro-2563EB?style=for-the-badge" alt="Badge Version 2.0">
    <img src="https://img.shields.io/badge/OS-Android_9%2B-3D6A9E?style=for-the-badge&logo=android" alt="Badge Android">
    <img src="https://img.shields.io/badge/Connexion-Bluetooth_HID_60Hz-0082FC?style=for-the-badge&logo=bluetooth" alt="Badge Bluetooth">
    <img src="https://img.shields.io/badge/Concepteur-MaxAdis-10B981?style=for-the-badge" alt="Badge Concepteur">
  </div>
</div>

---

Bienvenue sur le dépôt officiel de **MaNette**, l'application conçue par **MaxAdis** qui transforme votre smartphone Android en un contrôleur Bluetooth HID natif ultra-réactif pour PC, sans aucun serveur ni pilote tiers à installer.

## 🆕 Nouveautés de la Version 2.0 (`MaNette-v2.apk`)

*   🕹️ **Sticks Analogiques 3D Photoréalistes** : Dômes concaves 3D qui se déplacent physiquement à 360° dans leur cavité avec ombre portée dynamique, anneau spéculaire et retour élastique au centre.
*   🎯 **Effets d'Enfoncement Calibrés au Pixel Près (`1024 × 571`)** :
    *   **Gâchettes `L` et `R` Incurvées** : Arche ergonomique courbe suivant exactement le contour supérieur bleu de la manette.
    *   **Boutons `Y`, `X`, `B`, `A` Agrandis** : Effet d'enfoncement circulaire verrouillé sur la face supérieure de chaque touche.
    *   **Croix Directionnelle (`D-Pad`) & `SELECT` / `START`** : Assombrissement mécanique précis branche par branche uniquement lors de l'appui.
*   📳 **Retour Haptique Mécanique (API Vibration Android)** : Impulsions tactiles distinctes à l'enfoncement, au relâchement et en butée des sticks 3D, activables ou désactivables à tout moment dans les **Paramètres (`⚙`)**.
*   🏷️ **Signature & Crédits Intégrés** : Affichage de la **Version 2.0** et du concepteur **MaxAdis** directement dans l'application (Splash, Accueil, HUD Manette et boîte de dialogue *À propos*).
*   🌐 **Site Web Ultra-Moderne propulsé par Motion (`motion.dev`)** : Simulateur 3D interactif jouable dans le navigateur, démonstration vidéo 3D et téléchargement double version.

---

## 📦 Téléchargement des Versions (v2.0 & v1.0 conservée)

Les deux versions de l'application sont conservées dans ce dépôt afin que vous puissiez télécharger la nouvelle version **v2.0 Pro** tout en gardant accès à la version **v1.0 Classique** :

| Version | Fichier APK | Description |
| :--- | :--- | :--- |
| **🚀 MaNette v2.0 Pro (Recommandé)** | [`MaNette-v2.apk`](MaNette-v2.apk) | Sticks 3D animés, boutons agrandis, gâchettes L/R incurvées, retour haptique configurable, crédits MaxAdis. |
| **📦 MaNette v1.0 Classique** | [`MaNette.apk`](MaNette.apk) | Version originale (v1.0) conservée intacte. |

---

## 🎬 Aperçu Vidéo 3D (Version 2.0)

<div align="center">
  <img src="website/assets/demo_analogue_3d.gif" alt="Démo 3D MaNette v2.0" width="640" style="border-radius:16px;">
</div>

---

## 🚀 Comment l'utiliser ?

1.  **Téléchargez** `MaNette-v2.apk` (Version 2.0 Pro) ou `MaNette.apk` (Version 1.0) depuis ce dépôt ou le site web.
2.  **Installez** l'application sur votre smartphone Android (Android 9.0 / API 28+ requis pour le profil Bluetooth HID).
3.  **Associez** votre téléphone à votre ordinateur via les paramètres Bluetooth de votre PC.
4.  **Lancez** MaNette sur votre téléphone et appuyez sur **"Lancer la manette"**.
5.  **Jouez !** Votre PC détecte immédiatement une manette Bluetooth HID standard à 60Hz.

---

## 📁 Structure du projet

*   `/app` : Code source complet de l'application Android v2.0 (Kotlin, Custom Views 3D, Bluetooth HID Service, HapticManager).
*   `/website` : Site web promotionnel ultra-moderne avec animations **Motion (`motion.dev`)**, simulateur 3D interactif et téléchargement des versions `MaNette-v2.apk` et `MaNette.apk`.
*   `MaNette-v2.apk` : Nouvel exécutable Android **Version 2.0 Pro**.
*   `MaNette.apk` : Exécutable Android original **Version 1.0** (conservé).

## 👨‍💻 Concepteur & Auteur

Conçu et développé avec passion par **MaxAdis**.  
*L'application innovante qui métamorphose votre smartphone en un contrôleur PC ultra-réactif. Ne dépensez plus, jouez plus.*
