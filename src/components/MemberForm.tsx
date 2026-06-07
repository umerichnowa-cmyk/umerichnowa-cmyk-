import React, { useState } from 'react';
import { db } from '../lib/firebase';
import { collection, addDoc, serverTimestamp } from 'firebase/firestore';
import { MemberStatus, NATIONALITIES } from '../types';
import { Send, CheckCircle, User, Phone, MapPin, Calendar, BookOpen, GraduationCap } from 'lucide-react';

const compressImage = (base64Str: string, maxWidth = 600, maxHeight = 750): Promise<string> => {
  return new Promise((resolve) => {
    const img = new Image();
    img.src = base64Str;
    img.onload = () => {
      const canvas = document.createElement('canvas');
      let width = img.width;
      let height = img.height;

      // Maintain aspect ratio
      if (width > height) {
        if (width > maxWidth) {
          height = Math.round((height * maxWidth) / width);
          width = maxWidth;
        }
      } else {
        if (height > maxHeight) {
          width = Math.round((width * maxHeight) / height);
          height = maxHeight;
        }
      }

      canvas.width = width;
      canvas.height = height;

      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.drawImage(img, 0, 0, width, height);
        // Compress as JPEG with 0.75 quality for super lightweight output
        resolve(canvas.toDataURL('image/jpeg', 0.75));
      } else {
        resolve(base64Str);
      }
    };
    img.onerror = () => {
      resolve(base64Str);
    };
  });
};

export const MemberForm: React.FC = () => {
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    address: '',
    dateOfBirth: '',
    nationality: 'ci',
    memberType: 'EXPERT' as string,
    educationLevel: '',
    specialization: '',
    functionRole: '',
    photoUrl: '', // Will hold base64
  });
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const generateMatricule = (nationality: string) => {
    const year = new Date().getFullYear();
    const random = Math.random().toString(36).substring(2, 8).toUpperCase();
    return `AEIDC-${year}-${random}-${nationality.toUpperCase()}`;
  };

  const handlePhotoChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (file.size > 16 * 1024 * 1024) {
        alert("La photo est trop volumineuse (max 16MB)");
        return;
      }
      const reader = new FileReader();
      reader.onloadend = () => {
        const base64 = reader.result as string;
        compressImage(base64).then((compressed) => {
          setFormData(prev => ({ ...prev, photoUrl: compressed }));
        });
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    
    try {
      const matricule = generateMatricule(formData.nationality);
      const fullName = `${formData.firstName} ${formData.lastName}`;
      
      await addDoc(collection(db, 'members'), {
        ...formData,
        fullName,
        matricule,
        expirationDate: '31/12/2026',
        createdAt: serverTimestamp(),
      });
      setSubmitted(true);
    } catch (error) {
      console.error("Error creating member:", error);
      alert("Une erreur est survenue lors de l'enregistrement.");
    } finally {
      setLoading(false);
    }
  };

  if (submitted) {
    return (
      <div className="glass-panel p-8 rounded-3xl text-center flex flex-col items-center gap-6 animate-in zoom-in duration-500 max-w-lg mx-auto">
        <div className="w-20 h-20 bg-secondary/20 rounded-full flex items-center justify-center">
          <CheckCircle className="w-12 h-12 text-secondary" />
        </div>
        <div className="space-y-2">
          <h2 className="text-3xl font-black text-secondary uppercase tracking-tight">Inscription Réussie</h2>
          <p className="text-slate-400">
            Votre demande d'adhésion à l'AEIDC a été enregistrée avec succès. 
            Votre carte est prête à être émise par l'administration.
          </p>
        </div>
        <button 
          onClick={() => setSubmitted(false)}
          className="mt-4 px-8 py-3 bg-secondary text-[#0a1628] font-black rounded-xl hover:scale-105 transition-all shadow-lg uppercase tracking-widest text-sm"
        >
          Nouvelle Inscription
        </button>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="glass-panel p-8 md:p-12 rounded-[2.5rem] space-y-8 max-w-2xl w-full mx-auto shadow-2xl animate-in slide-in-from-bottom-8 duration-700 border-t-4 border-[#b8860b]">
      <div className="space-y-2 border-b border-white/10 pb-6">
        <h2 className="text-3xl font-black text-white tracking-tight flex items-center gap-3">
          <div className="p-2 bg-[#b8860b]/10 rounded-lg">
            <User className="w-6 h-6 text-[#d4af37]" />
          </div>
          Nouveau Membre <span className="text-[#d4af37]">AEIDC</span>
        </h2>
        <p className="text-slate-400 text-sm">Formulaire officiel d'inscription pour l'émission de la carte de membre.</p>
      </div>

      <div className="space-y-6">
        <div className="space-y-5">
          <h3 className="text-[10px] font-black uppercase text-[#d4af37] tracking-[0.2em] flex items-center gap-2 mb-2">
            INFORMATIONS PERSONNELLES
          </h3>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-[10px] font-black uppercase text-slate-500 tracking-wider">Prénom *</label>
              <input 
                required
                value={formData.firstName}
                onChange={(e) => setFormData({...formData, firstName: e.target.value})}
                className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm focus:ring-1 focus:ring-[#d4af37] outline-none transition-all placeholder:text-slate-700"
                placeholder="Jean"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-[10px] font-black uppercase text-slate-500 tracking-wider">Nom *</label>
              <input 
                required
                value={formData.lastName}
                onChange={(e) => setFormData({...formData, lastName: e.target.value})}
                className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm focus:ring-1 focus:ring-[#d4af37] outline-none transition-all placeholder:text-slate-700"
                placeholder="Dupont"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-[10px] font-black uppercase text-slate-500 tracking-wider">Email *</label>
              <input 
                required
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({...formData, email: e.target.value})}
                className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm focus:ring-1 focus:ring-[#d4af37] outline-none transition-all placeholder:text-slate-700"
                placeholder="jean.dupont@email.com"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-[10px] font-black uppercase text-slate-500 tracking-wider">Téléphone</label>
              <input 
                value={formData.phone}
                onChange={(e) => setFormData({...formData, phone: e.target.value})}
                className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm focus:ring-1 focus:ring-[#d4af37] outline-none transition-all placeholder:text-slate-700"
                placeholder="+237 6XX XX XX XX"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-[10px] font-black uppercase text-slate-500 tracking-wider">Date de Naissance</label>
              <input 
                type="date"
                value={formData.dateOfBirth}
                onChange={(e) => setFormData({...formData, dateOfBirth: e.target.value})}
                className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm focus:ring-1 focus:ring-[#d4af37] outline-none"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-[10px] font-black uppercase text-slate-500 tracking-wider">Nationalité</label>
              <select 
                value={formData.nationality}
                onChange={(e) => setFormData({...formData, nationality: e.target.value})}
                className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm focus:ring-1 focus:ring-[#d4af37] outline-none"
              >
                <option value="">-- Sélectionner --</option>
                {Object.entries(NATIONALITIES).map(([code, name]) => (
                  <option key={code} value={code} className="bg-[#0a1628]">{name}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="text-[10px] font-black uppercase text-slate-500 tracking-wider">Adresse</label>
            <input 
              value={formData.address}
              onChange={(e) => setFormData({...formData, address: e.target.value})}
              className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm focus:ring-1 focus:ring-[#d4af37] outline-none placeholder:text-slate-700"
              placeholder="Adresse complète"
            />
          </div>
        </div>

        <div className="space-y-5">
          <h3 className="text-[10px] font-black uppercase text-[#d4af37] tracking-[0.2em] flex items-center gap-2 mb-2">
            TYPE DE MEMBRE *
          </h3>
          <div className="flex flex-wrap gap-4">
            {[
              { id: 'EXPERT', label: 'Expert AEIDC' },
              { id: 'TUTEUR', label: 'Tuteur AEIDC' },
              { id: 'ACTIF', label: 'Membre Actif' },
              { id: 'STAGIAIRE', label: 'Stagiaire AEIDC' },
              { id: 'ETUDIANT', label: 'Étudiant' },
              { id: 'PERSONNEL', label: 'Personnel AEIDC' }
            ].map((type) => (
              <label key={type.id} className="flex items-center gap-2 cursor-pointer group">
                <input 
                  type="radio"
                  name="member_type"
                  value={type.id}
                  checked={formData.memberType === type.id}
                  onChange={(e) => setFormData({...formData, memberType: e.target.value})}
                  className="hidden"
                />
                <div className={`w-4 h-4 rounded-full border-2 transition-all flex items-center justify-center ${formData.memberType === type.id ? 'border-[#d4af37] bg-[#d4af37]/20' : 'border-white/20'}`}>
                  {formData.memberType === type.id && <div className="w-2 h-2 rounded-full bg-[#d4af37]" />}
                </div>
                <span className={`text-sm font-bold transition-colors ${formData.memberType === type.id ? 'text-[#d4af37]' : 'text-slate-400 group-hover:text-white'}`}>
                  {type.label}
                </span>
              </label>
            ))}
          </div>
        </div>

        {formData.memberType === 'ETUDIANT' && (
          <div className="space-y-5 animate-in slide-in-from-top-4 duration-500">
            <h3 className="text-[10px] font-black uppercase text-[#d4af37] tracking-[0.2em] flex items-center gap-2 mb-2">
              INFORMATIONS ACADÉMIQUES (ÉTUDIANT)
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="text-[10px] font-black uppercase text-slate-500 tracking-wider">Niveau d'Études</label>
                <select 
                  value={formData.educationLevel}
                  onChange={(e) => setFormData({...formData, educationLevel: e.target.value})}
                  className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm focus:ring-1 focus:ring-[#d4af37]"
                >
                  <option value="">-- Sélectionner le niveau --</option>
                  <option value="Licence 1" className="bg-[#0a1628]">Licence 1 (L1)</option>
                  <option value="Licence 2" className="bg-[#0a1628]">Licence 2 (L2)</option>
                  <option value="Licence 3" className="bg-[#0a1628]">Licence 3 (L3)</option>
                  <option value="Master 1" className="bg-[#0a1628]">Master 1 (M1)</option>
                  <option value="Master 2" className="bg-[#0a1628]">Master 2 (M2)</option>
                  <option value="Doctorat" className="bg-[#0a1628]">Doctorat (PhD)</option>
                </select>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-black uppercase text-slate-500 tracking-wider">Domaine de Spécialisation</label>
                <input 
                  value={formData.specialization}
                  onChange={(e) => setFormData({...formData, specialization: e.target.value})}
                  className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm focus:ring-1 focus:ring-[#d4af37] placeholder:text-slate-700"
                  placeholder="Ex: Génie Informatique, IA, etc."
                />
              </div>
            </div>
          </div>
        )}

        <div className="space-y-5">
          <h3 className="text-[10px] font-black uppercase text-[#d4af37] tracking-[0.2em] flex items-center gap-2 mb-2">
            FONCTION/RÔLE
          </h3>
          <div className="space-y-1.5">
            <label className="text-[10px] font-black uppercase text-slate-500 tracking-wider">
              {formData.memberType === 'ETUDIANT' ? 'Fonction ou Rôle à l\'AEIDC (Optionnel pour Étudiant)' : 'Fonction ou Rôle à l\'AEIDC'}
            </label>
            <input 
              value={formData.functionRole}
              onChange={(e) => setFormData({...formData, functionRole: e.target.value})}
              className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm focus:ring-1 focus:ring-[#d4af37] placeholder:text-slate-700"
              placeholder="Ex: Ingénieur Expert, Formateur, etc."
            />
          </div>
        </div>

        <div className="space-y-5">
          <h3 className="text-[10px] font-black uppercase text-[#d4af37] tracking-[0.2em] flex items-center gap-2 mb-2">
            PHOTO DE PROFIL
          </h3>
          <div className="flex gap-6 items-start">
            <div className="flex-1 space-y-1.5">
              <label className="text-[10px] font-black uppercase text-slate-500 tracking-wider underline">Photo (Max 16 MB)</label>
              <div className="relative group overflow-hidden bg-white/5 border-2 border-dashed border-white/10 rounded-xl p-6 flex flex-col items-center justify-center hover:border-[#d4af37]/40 transition-all cursor-pointer">
                <input 
                  type="file"
                  accept="image/*"
                  onChange={handlePhotoChange}
                  className="absolute inset-0 opacity-0 cursor-pointer z-10"
                />
                <BookOpen className="w-8 h-8 text-slate-500 group-hover:text-[#d4af37] mb-2" />
                <p className="text-sm font-bold text-slate-400 group-hover:text-white text-center">Cliquez pour choisir une photo ou glissez-la ici</p>
                <p className="text-[10px] text-slate-500 mt-2 uppercase">Formats: JPG, PNG</p>
              </div>
            </div>
            {formData.photoUrl && (
              <div className="w-24 h-32 rounded-lg overflow-hidden border-2 border-[#d4af37] shrink-0 animate-in zoom-in duration-300">
                <img src={formData.photoUrl} alt="Preview" className="w-full h-full object-cover" />
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="flex flex-col md:flex-row gap-4 pt-4">
        <button 
          disabled={loading}
          type="submit"
          className="flex-1 py-4 bg-gradient-to-r from-[#b8860b] to-[#d4af37] text-[#0a1628] font-black rounded-2xl flex items-center justify-center gap-3 hover:scale-[1.02] active:scale-[0.98] transition-all disabled:opacity-50 shadow-2xl shadow-[#b8860b]/20 text-base uppercase tracking-widest"
        >
          {loading ? "TRAITEMENT EN COURS..." : (
            <>
              <Send className="w-5 h-5" />
              Créer la Carte
            </>
          )}
        </button>
        <button 
          type="button"
          onClick={() => window.history.back()}
          className="px-8 py-4 bg-white/5 text-slate-400 font-black rounded-2xl border border-white/5 hover:bg-white/10 hover:text-white transition-all uppercase tracking-widest text-sm"
        >
          Annuler
        </button>
      </div>
      
      <p className="text-[9px] text-center text-slate-600 font-bold uppercase tracking-widest">
        Validité du Titre : 31/12/2026. Audit Systématique par le Bureau des Experts.
      </p>
    </form>
  );
};
