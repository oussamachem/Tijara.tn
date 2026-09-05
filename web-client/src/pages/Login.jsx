import { useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import Header from '../components/Header.jsx';
import { Button } from '../components/ui.jsx';

/**
 * Connexion déléguée à Keycloak (SSO). « Se connecter » redirige vers la page Keycloak (thème
 * Smart Boutique) puis revient sur la marketplace. Aucun mot de passe n'est saisi ici.
 */
export default function Login() {
  const { login, register } = useAuth();
  const { state } = useLocation();
  const from = state?.from || '/';

  return (
    <div>
      <Header title="Connexion" back />
      <div className="space-y-4 p-4">
        <div className="py-2 text-center">
          <div className="text-4xl">👋</div>
          <p className="mt-1 text-sm text-slate-500">Connectez-vous pour commander et suivre vos achats.</p>
        </div>
        <Button className="w-full" type="button" onClick={() => login(from)}>Se connecter</Button>
        <Button className="w-full" variant="secondary" type="button" onClick={() => register()}>Créer un compte</Button>
        <p className="text-center text-xs text-slate-400">Connexion sécurisée via Keycloak (SSO)</p>
      </div>
    </div>
  );
}
