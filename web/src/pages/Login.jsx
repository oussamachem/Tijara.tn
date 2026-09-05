import { useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { Button } from '../components/ui.jsx';

/**
 * Page de connexion UNIFIÉE (SSO). L'authentification est déléguée à Keycloak : « Se connecter »
 * redirige vers la page Keycloak (thème personnalisé) puis revient sur l'app. Aucun mot de passe
 * n'est saisi ici. Après retour, c'est le contexte (memberships) qui décide de l'espace.
 */
export default function Login() {
  const { login, register } = useAuth();
  const { state } = useLocation();
  const from = state?.from || '/';

  return (
    <div className="flex min-h-full items-center justify-center bg-slate-100 p-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-8 shadow-lg">
        <div className="mb-6 text-center">
          <div className="text-3xl">🛍️</div>
          <h1 className="mt-2 text-xl font-bold text-slate-800">Smart Boutique</h1>
          <p className="text-sm text-slate-500">Un seul compte pour tout gérer</p>
        </div>

        <div className="space-y-3">
          <Button type="button" className="w-full" onClick={() => login(from)}>
            Se connecter
          </Button>
          <Button type="button" variant="secondary" className="w-full" onClick={() => register()}>
            Créer un compte
          </Button>
        </div>

        <p className="mt-6 text-center text-xs text-slate-400">
          Connexion sécurisée via Keycloak (SSO)
        </p>
      </div>
    </div>
  );
}
