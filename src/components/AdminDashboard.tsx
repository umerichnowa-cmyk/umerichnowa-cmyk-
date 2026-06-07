import React, { useEffect, useState } from 'react';
import { db } from '../lib/firebase';
import { collection, query, orderBy, onSnapshot, deleteDoc, doc } from 'firebase/firestore';
import { Member, FLAG_EMOJIS, NATIONALITIES } from '../types';
import { Trash2, Grid, List as ListIcon, Search, Users, Star, GraduationCap, ShieldCheck, User, CheckCircle } from 'lucide-react';
import { MemberCard } from './MemberCard';

export const AdminDashboard: React.FC = () => {
  const [members, setMembers] = useState<Member[]>([]);
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('list');
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);

  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);

  useEffect(() => {
    console.log("Initialisation du tableau de bord admin...");
    const q = query(collection(db, 'members'), orderBy('createdAt', 'desc'));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const memberData = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      })) as Member[];
      setMembers(memberData);
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const handleDelete = async (id: string) => {
    if (!id) {
      console.error("Tentative de suppression sans ID");
      return;
    }

    if (confirmDeleteId !== id) {
      setConfirmDeleteId(id);
      // Reset after 3 seconds if not confirmed
      setTimeout(() => setConfirmDeleteId(prev => prev === id ? null : prev), 3000);
      return;
    }

    try {
      console.log("Début de la suppression du membre:", id);
      await deleteDoc(doc(db, 'members', id));
      console.log("Membre supprimé avec succès de Firestore:", id);
      setConfirmDeleteId(null);
    } catch (error) {
      console.error("Erreur critique lors de la suppression:", error);
      alert("Erreur de suppression. Vérifiez vos permissions console.");
      setConfirmDeleteId(null);
    }
  };

  const getMemberTypeLabel = (type: string) => {
    switch (type) {
      case 'EXPERT': return 'Expert AEIDC';
      case 'ACTIF': return 'Membre Actif';
      case 'PERSONNEL': return 'Personnel AEIDC';
      case 'STAGIAIRE': return 'Stagiaire AEIDC';
      case 'TUTEUR': return 'Tuteur AEIDC';
      case 'ETUDIANT': return 'Étudiant';
      default: return type;
    }
  };

  const filteredMembers = members.filter(m => 
    m.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    m.matricule.toLowerCase().includes(searchTerm.toLowerCase()) ||
    m.email.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const stats = {
    total: members.length,
    experts: members.filter(m => m.memberType === 'EXPERT').length,
    students: members.filter(m => m.memberType === 'ETUDIANT').length,
    actives: members.filter(m => m.memberType === 'ACTIF').length,
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center p-20">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary"></div>
      </div>
    );
  }

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
        <div className="space-y-1">
          <h2 className="text-3xl font-black text-white tracking-tight flex items-center gap-3">
            <Users className="w-8 h-8 text-[#d4af37]" />
            Liste des <span className="text-[#d4af37]">Membres</span>
          </h2>
          <p className="text-slate-400 text-sm">Gestion centralisée des adhésions et titres de transport AEIDC.</p>
        </div>
        <div className="flex bg-white/5 p-1 rounded-xl border border-white/10">
          <button 
            onClick={() => setViewMode('list')}
            className={`p-2 rounded-lg transition-all ${viewMode === 'list' ? 'bg-[#d4af37] text-[#0a1628] shadow-lg' : 'text-slate-400 hover:text-white'}`}
          >
            <ListIcon className="w-5 h-5" />
          </button>
          <button 
            onClick={() => setViewMode('grid')}
            className={`p-2 rounded-lg transition-all ${viewMode === 'grid' ? 'bg-[#d4af37] text-[#0a1628] shadow-lg' : 'text-slate-400 hover:text-white'}`}
          >
            <Grid className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Stats Section */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard icon={<Users className="text-[#3b82f6]" />} label="Total" value={stats.total} />
        <StatCard icon={<Star className="text-[#eab308]" />} label="Experts" value={stats.experts} />
        <StatCard icon={<GraduationCap className="text-[#a78bfa]" />} label="Étudiants" value={stats.students} />
        <StatCard icon={<ShieldCheck className="text-[#22c55e]" />} label="Actifs" value={stats.actives} />
      </div>

      <div className="glass-panel p-6 rounded-3xl space-y-6">
        <div className="relative w-full max-w-md">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
          <input 
            type="text"
            placeholder="Rechercher un membre..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full bg-white/5 border border-white/10 rounded-2xl pl-12 pr-4 py-3 text-sm focus:ring-1 focus:ring-[#d4af37] outline-none transition-all"
          />
        </div>

        {viewMode === 'grid' ? (
          <div className="grid grid-cols-1 gap-12 w-full max-w-4xl mx-auto justify-items-center">
            {filteredMembers.map(member => (
              <div key={member.id} className="relative group p-6 bg-white/[0.01] border border-white/5 rounded-[2px] rounded-[2.5rem] hover:bg-white/[0.03] hover:border-[#d4af37]/20 transition-all flex flex-col items-center w-full shadow-2xl">
                <div className="absolute top-6 right-6 z-20">
                  <button 
                    onClick={() => handleDelete(member.id!)}
                    className={`p-2.5 rounded-xl transition-all shadow-xl ${confirmDeleteId === member.id ? 'bg-red-500 text-white animate-pulse' : 'bg-red-500/10 text-red-500 hover:bg-red-500 hover:text-white'}`}
                    title={confirmDeleteId === member.id ? "Cliquez à nouveau pour confirmer" : "Supprimer"}
                  >
                    <Trash2 className="w-5 h-5" />
                  </button>
                </div>
                <div className="mb-6 text-center">
                  <p className="text-[10px] font-black text-[#d4af37] tracking-[0.2em] uppercase mb-1">CARTE GÉNÉRÉE</p>
                  <p className="text-lg font-extrabold text-white">{member.fullName}</p>
                </div>
                <MemberCard member={member} />
              </div>
            ))}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full border-collapse">
              <thead>
                <tr className="text-left border-b border-white/10">
                  <th className="px-6 py-4 text-[10px] font-black text-slate-500 uppercase tracking-widest">Membre</th>
                  <th className="px-6 py-4 text-[10px] font-black text-slate-500 uppercase tracking-widest">Numero</th>
                  <th className="px-6 py-4 text-[10px] font-black text-slate-500 uppercase tracking-widest">Type</th>
                  <th className="px-6 py-4 text-[10px] font-black text-slate-500 uppercase tracking-widest">Statut</th>
                  <th className="px-6 py-4 text-[10px] font-black text-slate-500 uppercase tracking-widest">Expire le</th>
                  <th className="px-6 py-4 text-[10px] font-black text-slate-500 uppercase tracking-widest text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {filteredMembers.map(member => (
                  <tr key={member.id} className="hover:bg-white/[0.02] transition-all group">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-4">
                        <div className="w-10 h-10 bg-gradient-to-br from-[#b8860b] to-[#d4af37] rounded-full overflow-hidden border-2 border-[#d4af37] shrink-0 p-0.5">
                          {member.photoUrl ? (
                            <img src={member.photoUrl} alt="" className="w-full h-full object-cover rounded-full" />
                          ) : (
                            <div className="w-full h-full flex items-center justify-center bg-[#0a1628] rounded-full">
                              <User className="w-5 h-5 text-[#d4af37]" />
                            </div>
                          )}
                        </div>
                        <div>
                          <p className="font-bold text-white text-sm leading-none mb-1">{member.fullName}</p>
                          <p className="text-[10px] text-slate-500 font-medium">{member.email}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <code className="text-[10px] bg-white/5 text-slate-300 px-2 py-1 rounded font-mono">
                        {member.matricule.substring(0, 18)}...
                      </code>
                    </td>
                    <td className="px-6 py-4">
                       <span className="text-[10px] font-bold text-slate-300 uppercase tracking-tight">{getMemberTypeLabel(member.memberType)}</span>
                    </td>
                    <td className="px-6 py-4">
                       <span className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-green-500/10 text-green-500 text-[10px] font-black rounded-full uppercase">
                          <CheckCircle className="w-3 h-3" />
                          Actif
                       </span>
                    </td>
                    <td className="px-6 py-4">
                       <span className="text-[11px] font-bold text-slate-400">31/12/2026</span>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex justify-end gap-2">
                        <button 
                           onClick={() => setViewMode('grid')} // Small trick to "view" card
                           className="p-2 bg-white/5 text-slate-400 hover:text-[#d4af37] hover:bg-[#d4af37]/10 rounded-lg transition-all"
                           title="Voir la carte"
                        >
                           <ShieldCheck className="w-4 h-4" />
                        </button>
                        <button 
                           className="p-2 bg-white/5 text-slate-400 hover:text-white hover:bg-white/10 rounded-lg transition-all"
                           title="Modifier"
                        >
                           <Search className="w-4 h-4" />
                        </button>
                        <button 
                          onClick={() => handleDelete(member.id!)}
                          className={`p-2 rounded-lg transition-all ${confirmDeleteId === member.id ? 'bg-red-500 text-white animate-pulse' : 'bg-white/5 text-slate-400 hover:text-red-500 hover:bg-red-500/10'}`}
                          title={confirmDeleteId === member.id ? "Confirmer la suppression" : "Supprimer"}
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

const StatCard: React.FC<{ icon: React.ReactNode, label: string, value: number }> = ({ icon, label, value }) => (
  <div className="glass-panel p-6 rounded-3xl space-y-4 border border-white/5 hover:border-[#d4af37]/20 transition-all group shadow-lg">
    <div className="flex justify-between items-start">
      <div className="p-3 bg-white/5 rounded-2xl group-hover:scale-110 transition-transform">
        {icon}
      </div>
      <span className="text-3xl font-black text-white">{value}</span>
    </div>
    <p className="text-xs font-black text-slate-500 uppercase tracking-widest">{label}</p>
  </div>
);

