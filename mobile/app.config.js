// Configuration Expo. L'URL de l'API est injectée via la variable d'environnement
// API_BASE_URL (jamais codée en dur). Défaut = 10.0.2.2:8080 (= localhost de la machine
// hôte vu depuis l'émulateur Android). Sur device physique, utiliser l'IP LAN du backend.
export default () => ({
  expo: {
    name: 'Smart Boutique Vendeur',
    slug: 'smart-boutique-mobile',
    version: '0.1.0',
    orientation: 'portrait',
    userInterfaceStyle: 'light',
    splash: { backgroundColor: '#0f172a' },
    assetBundlePatterns: ['**/*'],
    ios: { supportsTablet: false },
    android: {
      package: 'com.smartboutique.mobile',
    },
    plugins: [
      [
        'expo-camera',
        {
          cameraPermission:
            'Autoriser Smart Boutique à utiliser la caméra pour scanner les QR codes produits.',
        },
      ],
    ],
    extra: {
      apiBaseUrl: process.env.API_BASE_URL || 'http://10.0.2.2:8080',
    },
  },
});
