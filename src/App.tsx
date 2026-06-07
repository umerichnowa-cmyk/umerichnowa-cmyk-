import { useState, useEffect } from 'react';
import { auth, signInWithGoogle, logOut, testConnection } from './lib/firebase';
import { onAuthStateChanged, User } from 'firebase/auth';
import { MemberForm } from './components/MemberForm';
import { AdminDashboard } from './components/AdminDashboard';
import { ShieldCheck, LogIn, LogOut, LayoutDashboard, UserPlus, Globe } from 'lucide-react';

type View = 'HOME' | 'ENROLL' | 'ADMIN';

export default function App() {
  const [user, setUser] = useState<User | null>(null);
  const [view, setView] = useState<View>('HOME');
  const [authLoading, setAuthLoading] = useState(true);

  const isAdmin = user?.email === 'umerichnowa@gmail.com';

  useEffect(() => {
    testConnection();
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
      setAuthLoading(false);
    });
    return () => unsubscribe();
  }, []);

  const handleSignOut = async () => {
    await logOut();
    setView('HOME');
  };

  if (authLoading) {
    return (
      <div className="min-h-screen bg-surface flex items-center justify-center">
        <div className="animate-pulse flex flex-col items-center gap-4">
          <ShieldCheck className="w-12 h-12 text-primary" />
          <p className="label-micro animate-bounce">Chargement du Registre...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col">
      {/* Navigation */}
      <nav className="fixed top-0 left-0 right-0 z-50 glass-panel h-16 px-6 md:px-12 flex items-center justify-between border-none shadow-lg shadow-navy-950/20">
        <div className="flex items-center gap-3 cursor-pointer" onClick={() => setView('HOME')}>
          <div className="w-8 h-8 md:w-10 md:h-10 rounded-lg bg-secondary flex items-center justify-center shadow-lg shadow-secondary/20">
            <ShieldCheck className="w-5 h-5 md:w-6 md:h-6 text-[#0a1628]" />
          </div>
          <div>
            <h1 className="text-lg md:text-xl font-bold tracking-tight text-white leading-tight">AEIDC <span className="text-secondary">Cartes Pro</span></h1>
            <p className="text-[9px] md:text-[10px] uppercase tracking-wider font-bold text-slate-400">Système de Cartes Premium</p>
          </div>
        </div>

        <div className="hidden md:flex items-center gap-8 text-sm font-semibold text-slate-400">
           <button 
            onClick={() => setView('HOME')}
            className={`flex items-center gap-2 transition-all hover:text-white ${view === 'HOME' ? 'text-secondary' : ''}`}
           >
             <Globe className="w-4 h-4" /> Accueil
           </button>
           <button 
            onClick={() => setView('ENROLL')}
            className={`flex items-center gap-2 transition-all hover:text-white ${view === 'ENROLL' ? 'text-secondary' : ''}`}
           >
             <UserPlus className="w-4 h-4" /> Nouveau
           </button>
           {isAdmin && (
             <button 
              onClick={() => setView('ADMIN')}
              className={`flex items-center gap-2 transition-all hover:text-white ${view === 'ADMIN' ? 'text-secondary' : ''}`}
             >
               <LayoutDashboard className="w-4 h-4" /> Membres
             </button>
           )}
        </div>

        <div className="flex items-center gap-4">
          {user ? (
            <div className="flex items-center gap-3">
              <div className="hidden sm:block text-right">
                <p className="text-xs font-bold text-white truncate w-24 md:w-32">{user.displayName || user.email}</p>
                <p className="text-[10px] text-primary uppercase font-bold tracking-widest">
                  {isAdmin ? 'Accès Haute Autorité' : 'Membre'}
                </p>
              </div>
              <button 
                onClick={handleSignOut}
                className="p-2.5 rounded-xl bg-surface-container-highest text-slate-400 hover:text-white transition-all shadow-xl"
                title="Déconnexion"
              >
                <LogOut className="w-5 h-5" />
              </button>
            </div>
          ) : (
            <button 
              onClick={signInWithGoogle}
              className="px-5 py-2 md:py-2.5 rounded-xl bg-primary text-on-primary font-bold text-sm flex items-center gap-2 hover:scale-[1.02] active:scale-[0.98] transition-all"
            >
              <LogIn className="w-5 h-5" />
              Connexion Admin
            </button>
          )}
        </div>
      </nav>

      {/* Main Content */}
      <main className="flex-1 pt-24 pb-12 px-6 md:px-12 container mx-auto">
        {view === 'HOME' && (
          <div className="max-w-4xl mx-auto py-12 space-y-16">
            <div className="text-center space-y-6">
              <div className="inline-flex p-4 md:p-6 bg-white/5 rounded-3xl mb-4 border border-white/10 shadow-2xl skew-y-1">
                 <ShieldCheck className="w-16 h-16 md:w-24 md:h-24 text-secondary" />
              </div>
              <h1 className="text-4xl md:text-6xl font-extrabold tracking-tight text-white leading-[1.1]">
                Système de Cartes <br/>
                <span className="text-secondary italic">Professionnelles AEIDC</span>
              </h1>
              <p className="text-slate-400 text-base md:text-xl max-w-2xl mx-auto leading-relaxed">
                Génération de cartes de membres premium pour experts, stagiaires et étudiants.
                Design haute fidélité, validation QR et impression optimisée.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-8 translate-y-4 max-w-3xl mx-auto">
              <div 
                onClick={() => setView('ENROLL')}
                className="glass-panel p-8 rounded-3xl hover:bg-white/5 transition-all group cursor-pointer border-none shadow-xl"
              >
                <div className="w-16 h-16 rounded-2xl bg-secondary/10 flex items-center justify-center text-secondary mb-6 group-hover:scale-110 transition-transform">
                  <UserPlus className="w-8 h-8" />
                </div>
                <h3 className="text-xl font-bold text-white mb-3">Nouvelle Adhésion</h3>
                <p className="text-slate-400 text-sm leading-relaxed mb-6">
                  Remplissez le formulaire pour enrôler un membre et générer sa carte immédiatement.
                </p>
                <div className="w-full py-4 bg-secondary text-[#0a1628] rounded-xl font-bold text-center">
                  Créer une Carte
                </div>
              </div>

              <div 
                onClick={() => isAdmin ? setView('ADMIN') : signInWithGoogle()}
                className="glass-panel p-8 rounded-3xl flex flex-col justify-between border-white/10 shadow-xl hover:bg-white/5 transition-all group cursor-pointer"
              >
                <div>
                  <div className="w-16 h-16 rounded-2xl bg-white/5 flex items-center justify-center text-secondary mb-6 group-hover:scale-110 transition-transform">
                    <LayoutDashboard className="w-8 h-8" />
                  </div>
                  <h3 className="text-xl font-bold text-white mb-3">Registre des Membres</h3>
                  <p className="text-slate-400 text-sm leading-relaxed">
                    Consultez la liste des membres, imprimez les cartes ou gérez les adhésions existantes.
                  </p>
                </div>
                <div className="mt-6 w-full py-4 bg-white/5 text-white rounded-xl font-bold text-center border border-white/5">
                  Voir la Liste
                </div>
              </div>
            </div>
            
            <div className="p-8 rounded-3xl bg-white/5 border border-white/5 flex flex-col md:flex-row items-center justify-between gap-6">
               <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-full bg-green-500/10 flex items-center justify-center text-green-400">
                    <ShieldCheck className="w-6 h-6" />
                  </div>
                  <div>
                    <h4 className="font-bold text-white">Audit d'Intégrité du Registre</h4>
                    <p className="text-sm text-slate-400">Session sécurisée et archivage cryptographique actif.</p>
                  </div>
               </div>
               <div className="px-4 py-2 bg-green-500/10 rounded-full border border-green-500/20">
                 <span className="text-[10px] font-black uppercase text-green-400">Hachage Actif: 0x7F2a...98B1</span>
               </div>
            </div>
          </div>
        )}

        {view === 'ENROLL' && (
          <div className="flex items-center justify-center min-h-[60vh] animate-in fade-in slide-in-from-bottom-8 duration-700">
            <MemberForm />
          </div>
        )}

        {view === 'ADMIN' && (
          isAdmin ? <AdminDashboard /> : (
            <div className="text-center p-20 glass-panel rounded-2xl">
               <h2 className="title-display">Accès Refusé</h2>
               <p className="text-slate-400 mt-4">Désolé, seul l'Administrateur Souverain peut accéder à cet écran.</p>
               <button onClick={() => setView('HOME')} className="mt-6 text-secondary font-bold">Retour à l'accueil</button>
            </div>
          )
        )}
      </main>

      {/* Footer */}
      <footer className="py-8 px-6 md:px-12 flex flex-col md:flex-row items-center justify-between border-t border-white/5 bg-surface text-slate-500 text-xs gap-4">
        <div className="flex items-center gap-2">
           <ShieldCheck className="w-4 h-4 text-secondary" />
           <p>© 2026 AEIDC - Tous droits réservés.</p>
        </div>
        <div className="flex gap-6 uppercase font-bold tracking-widest text-[9px]">
           <button className="hover:text-secondary transition-colors">Politique de Confidentialité</button>
           <button className="hover:text-secondary transition-colors">Termes du Registre</button>
           <button className="hover:text-secondary transition-colors">Support Souverain</button>
        </div>
      </footer>
    </div>
  );
}
