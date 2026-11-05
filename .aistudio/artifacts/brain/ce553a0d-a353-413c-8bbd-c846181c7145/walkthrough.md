# Vidéo & Démonstration — Boutons Photoréalistes Agrandis, Effet d'Enfoncement Visible & Retour Haptique (`MaNette`)

## 1. Vidéo complète du test (Sticks 3D au centre + Effets d'enfoncement visibles + Vibrations haptiques)

![Vidéo de démonstration MaNette](/.aistudio/artifacts/brain/ce553a0d-a353-413c-8bbd-c846181c7145/demo_analogue_3d.gif)

- **Fichier vidéo MP4** : `demo_analogue_3d.mp4` (également disponible à la racine des fichiers du projet)
- **Animation GIF** : `demo_analogue_3d.gif`

---

## 2. État au repos (Boutons photoréalistes d'origine agrandis, surcouches 100 % invisibles)

![Manette au repos](/.aistudio/artifacts/brain/ce553a0d-a353-413c-8bbd-c846181c7145/manette_new_layout_rest.png)

---

## 3. État lors de l'appui (Seul l'effet d'enfoncement est visible sur les touches pressées + inclinaison 3D des sticks)

![Manette avec boutons enfoncés](/.aistudio/artifacts/brain/ce553a0d-a353-413c-8bbd-c846181c7145/manette_new_layout_pressed.png)

---

## 4. Retour Haptique Android (`Vibrator` / `VibrationEffect`) & Paramètres
- **Sur chaque bouton (`Y, B, A, X, L, R, SELECT, START`) et sur le `D-Pad`** : Vibration mécanique franche à l'enfoncement (`vibrateButtonPress` / `vibrateDPadPress`) et léger tic mécanique au relâchement (`vibrateButtonRelease`).
- **Sur les deux sticks analogiques 3D (`leftJoystick`, `rightJoystick`)** : Vibration au contact du pouce (`vibrateStickGrab`), vibration de butée contre le rebord de la cavité (`vibrateStickRimHit`), et retour élastique au centre (`vibrateButtonRelease`).
- **Désactivation dans les Paramètres (`⚙`)** :
  - Depuis l'**écran d'accueil (`HomeActivity`)** : Carte **« ⚙ Paramètres — Retour haptique »** avec interrupteur direct + boîte de dialogue de paramètres.
  - Depuis l'**écran de la manette (`GamepadActivity`)** : Bouton **`⚙`** dans la barre supérieure permettant d'activer ou désactiver les vibrations en plein jeu.
