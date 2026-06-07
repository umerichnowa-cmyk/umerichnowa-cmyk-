export type MemberStatus = 'ACTIF' | 'STAGIAIRE' | 'ETUDIANT' | 'EXPERT' | 'PERSONNEL' | 'TUTEUR';

export interface Member {
  id?: string;
  matricule: string;
  firstName: string;
  lastName: string;
  fullName: string; // derived or stored
  email: string;
  phone?: string;
  address?: string;
  dateOfBirth?: string;
  nationality: string;
  memberType: MemberStatus;
  educationLevel?: string;
  specialization?: string;
  functionRole?: string;
  photoUrl?: string; // base64 or URL
  createdAt: any;
  expirationDate: string;
}

export const NATIONALITIES: Record<string, string> = {
  'af': 'Afghanistan', 'al': 'Albanie', 'dz': 'Algérie', 'ad': 'Andorre', 'ao': 'Angola',
  'ar': 'Argentine', 'am': 'Arménie', 'au': 'Australie', 'at': 'Autriche', 'az': 'Azerbaïdjan',
  'bs': 'Bahamas', 'bh': 'Bahreïn', 'bd': 'Bangladesh', 'bb': 'Barbade', 'by': 'Biélorussie',
  'be': 'Belgique', 'bz': 'Belize', 'bj': 'Bénin', 'bt': 'Bhoutan', 'bo': 'Bolivie',
  'ba': 'Bosnie-Herzégovine', 'bw': 'Botswana', 'br': 'Brésil', 'bn': 'Brunei', 'bg': 'Bulgarie',
  'bf': 'Burkina Faso', 'bi': 'Burundi', 'kh': 'Cambodge', 'cm': 'Cameroun', 'ca': 'Canada',
  'cv': 'Cap-Vert', 'cf': 'Centrafrique', 'td': 'Tchad', 'cl': 'Chili', 'cn': 'Chine',
  'co': 'Colombie', 'km': 'Comores', 'cg': 'Congo', 'cd': 'RD Congo', 'cr': 'Costa Rica',
  'ci': "Côte d'Ivoire", 'hr': 'Croatie', 'cu': 'Cuba', 'cy': 'Chypre', 'cz': 'Tchéquie',
  'dk': 'Danemark', 'dj': 'Djibouti', 'dm': 'Dominique', 'do': 'Rép. Dominicaine', 'ec': 'Équateur',
  'eg': 'Égypte', 'sv': 'Salvador', 'gq': 'Guinée Équatoriale', 'er': 'Érythrée', 'ee': 'Estonie',
  'sz': 'Eswatini', 'et': 'Éthiopie', 'fj': 'Fidji', 'fi': 'Finlande', 'fr': 'France',
  'ga': 'Gabon', 'gm': 'Gambie', 'ge': 'Géorgie', 'de': 'Allemagne', 'gh': 'Ghana',
  'gr': 'Grèce', 'gd': 'Grenade', 'gt': 'Guatemala', 'gn': 'Guinée', 'gw': 'Guinée-Bissau',
  'gy': 'Guyana', 'ht': 'Haïti', 'hn': 'Honduras', 'hu': 'Hongrie', 'is': 'Islande',
  'in': 'Inde', 'id': 'Indonésie', 'ir': 'Iran', 'iq': 'Irak', 'ie': 'Irlande',
  'il': 'Israël', 'it': 'Italie', 'jm': 'Jamaïque', 'jp': 'Japon', 'jo': 'Jordanie',
  'kz': 'Kazakhstan', 'ke': 'Kenya', 'ki': 'Kiribati', 'kp': 'Corée du Nord', 'kr': 'Corée du Sud',
  'kw': 'Koweït', 'kg': 'Kirghizistan', 'la': 'Laos', 'lv': 'Lettonie', 'lb': 'Liban',
  'ls': 'Lesotho', 'lr': 'Libéria', 'ly': 'Libye', 'li': 'Liechtenstein', 'lt': 'Lituanie',
  'lu': 'Luxembourg', 'mg': 'Madagascar', 'mw': 'Malawi', 'my': 'Malaisie', 'mv': 'Maldives',
  'ml': 'Mali', 'mt': 'Malte', 'mh': 'Îles Marshall', 'mr': 'Mauritanie', 'mu': 'Maurice',
  'mx': 'Mexique', 'fm': 'Micronésie', 'md': 'Moldavie', 'mc': 'Monaco', 'mn': 'Mongolie',
  'me': 'Monténégro', 'ma': 'Maroc', 'mz': 'Mozambique', 'mm': 'Myanmar', 'na': 'Namibie',
  'nr': 'Nauru', 'np': 'Népal', 'nl': 'Pays-Bas', 'nz': 'Nouvelle-Zélande', 'ni': 'Nicaragua',
  'ne': 'Niger', 'ng': 'Nigeria', 'mk': 'Macédoine du Nord', 'no': 'Norvège', 'om': 'Oman',
  'pk': 'Pakistan', 'pw': 'Palaos', 'ps': 'Palestine', 'pa': 'Panama', 'pg': 'Papouasie-Nouvelle-Guinée',
  'py': 'Paraguay', 'pe': 'Pérou', 'ph': 'Philippines', 'pl': 'Pologne', 'pt': 'Portugal',
  'qa': 'Qatar', 'ro': 'Roumanie', 'ru': 'Russie', 'rw': 'Rwanda', 'kn': 'Saint-Kitts-et-Nevis',
  'lc': 'Sainte-Lucie', 'vc': 'Saint-Vincent', 'ws': 'Samoa', 'sm': 'Saint-Marin', 'st': 'Sao Tomé-et-Principe',
  'sa': 'Arabie Saoudite', 'sn': 'Sénégal', 'rs': 'Serbie', 'sc': 'Seychelles', 'sl': 'Sierra Leone',
  'sg': 'Singapour', 'sk': 'Slovaquie', 'si': 'Slovénie', 'sb': 'Îles Salomon', 'so': 'Somalie',
  'za': 'Afrique du Sud', 'ss': 'Soudan du Sud', 'es': 'Espagne', 'lk': 'Sri Lanka', 'sd': 'Soudan',
  'sr': 'Suriname', 'se': 'Suède', 'ch': 'Suisse', 'sy': 'Syrie', 'tw': 'Taïwan',
  'tj': 'Tadjikistan', 'tz': 'Tanzanie', 'th': 'Thaïlande', 'tl': 'Timor oriental', 'tg': 'Togo',
  'to': 'Tonga', 'tt': 'Trinité-et-Tobago', 'tn': 'Tunisie', 'tr': 'Turquie', 'tm': 'Turkménistan',
  'tv': 'Tuvalu', 'ug': 'Ouganda', 'ua': 'Ukraine', 'ae': 'Émirats Arabes Unis', 'gb': 'Royaume-Uni',
  'us': 'États-Unis', 'uy': 'Uruguay', 'uz': 'Ouzbékistan', 'vu': 'Vanuatu', 'va': 'Vatican',
  've': 'Venezuela', 'vn': 'Vietnam', 'ye': 'Yémen', 'zm': 'Zambie', 'zw': 'Zimbabwe',
  'other': 'Autre'
};

export const FLAG_EMOJIS: Record<string, string> = {
  'af': '🇦🇫', 'al': '🇦🇱', 'dz': '🇩🇿', 'ad': '🇦🇩', 'ao': '🇦🇴',
  'ar': '🇦🇷', 'am': '🇦🇲', 'au': '🇦🇺', 'at': '🇦🇹', 'az': '🇦🇿',
  'bs': '🇧🇸', 'bh': '🇧🇭', 'bd': '🇧🇩', 'bb': '🇧🇧', 'by': '🇧🇾',
  'be': '🇧🇪', 'bz': '🇧🇿', 'bj': '🇧🇯', 'bt': '🇧🇹', 'bo': '🇧🇴',
  'ba': '🇧🇦', 'bw': '🇧🇼', 'br': '🇧🇷', 'bn': '🇧🇳', 'bg': '🇧🇬',
  'bf': '🇧🇫', 'bi': '🇧🇮', 'kh': '🇰🇭', 'cm': '🇨🇲', 'ca': '🇨🇦',
  'cv': '🇨🇻', 'cf': '🇨🇫', 'td': '🇹🇩', 'cl': '🇨🇱', 'cn': '🇨🇳',
  'co': '🇨🇴', 'km': '🇰🇲', 'cg': '🇨🇬', 'cd': '🇨🇩', 'cr': '🇨🇷',
  'ci': '🇨🇮', 'hr': '🇭🇷', 'cu': '🇨🇺', 'cy': '🇨🇾', 'cz': '🇨🇿',
  'dk': '🇩🇰', 'dj': '🇩🇯', 'dm': '🇩🇲', 'do': '🇩🇴', 'ec': '🇪🇨',
  'eg': '🇪🇬', 'sv': '🇸🇻', 'gq': '🇬🇶', 'er': '🇪🇷', 'ee': '🇪🇪',
  'sz': '🇸🇿', 'et': '🇪🇹', 'fj': '🇫🇯', 'fi': '🇫🇮', 'fr': '🇫🇷',
  'ga': '🇬🇦', 'gm': '🇬🇲', 'ge': '🇬🇪', 'de': '🇩🇪', 'gh': '🇬🇭',
  'gr': '🇬🇷', 'gd': '🇬🇩', 'gt': '🇬🇹', 'gn': '🇬🇳', 'gw': '🇬🇼',
  'gy': '🇬🇾', 'ht': '🇭🇹', 'hn': '🇭🇳', 'hu': '🇭🇺', 'is': '🇮🇸',
  'in': '🇮🇳', 'id': '🇮🇩', 'ir': '🇮🇷', 'iq': '🇮🇶', 'ie': '🇮🇪',
  'il': '🇮🇱', 'it': '🇮🇹', 'jm': '🇯🇲', 'jp': '🇯🇵', 'jo': '🇯🇴',
  'kz': '🇰🇿', 'ke': '🇰🇪', 'ki': '🇰🇮', 'kp': '🇰🇵', 'kr': '🇰🇷',
  'kw': '🇰🇼', 'kg': '🇰🇬', 'la': '🇱🇦', 'lv': '🇱🇻', 'lb': '🇱🇧',
  'ls': '🇱🇸', 'lr': '🇱🇷', 'ly': '🇱🇾', 'li': '🇱🇮', 'lt': '🇱🇹',
  'lu': '🇱🇺', 'mg': '🇲🇬', 'mw': '🇲🇼', 'my': '🇲🇾', 'mv': '🇲🇻',
  'ml': '🇲🇱', 'mt': '🇲🇹', 'mh': '🇲🇭', 'mr': '🇲🇷', 'mu': '🇲🇺',
  'mx': '🇲🇽', 'fm': '🇫🇲', 'md': '🇲🇩', 'mc': '🇲🇨', 'mn': '🇲🇳',
  'me': '🇲🇪', 'ma': '🇲🇦', 'mz': '🇲🇿', 'mm': '🇲🇲', 'na': '🇳🇦',
  'nr': '🇳🇷', 'np': '🇳🇵', 'nl': '🇳🇱', 'nz': '🇳🇿', 'ni': '🇳🇮',
  'ne': '🇳🇪', 'ng': '🇳🇬', 'mk': '🇲🇰', 'no': '🇳🇴', 'om': '🇴🇲',
  'pk': '🇵🇰', 'pw': '🇵🇼', 'ps': '🇵🇸', 'pa': '🇵🇦', 'pg': '🇵🇬',
  'py': '🇵🇾', 'pe': '🇵🇪', 'ph': '🇵🇭', 'pl': '🇵🇱', 'pt': '🇵🇹',
  'qa': '🇶🇦', 'ro': '🇷🇴', 'ru': '🇷🇺', 'rw': '🇷🇼', 'kn': '🇰🇳',
  'lc': '🇱🇨', 'vc': '🇻🇨', 'ws': '🇼🇸', 'sm': '🇸🇲', 'st': '🇸🇹',
  'sa': '🇸🇦', 'sn': '🇸🇳', 'rs': '🇷🇸', 'sc': '🇸🇨', 'sl': '🇸🇱',
  'sg': '🇸🇬', 'sk': '🇸🇰', 'si': '🇸🇮', 'sb': '🇸🇧', 'so': '🇸🇴',
  'za': '🇿🇦', 'ss': '🇸🇸', 'es': '🇪🇸', 'lk': '🇱🇰', 'sd': '🇸🇩',
  'sr': '🇸🇷', 'se': '🇸🇪', 'ch': '🇨🇭', 'sy': '🇸🇾', 'tw': '🇹🇼',
  'tj': '🇹🇯', 'tz': '🇹🇿', 'th': '🇹🇭', 'tl': '🇹🇱', 'tg': '🇹🇬',
  'to': '🇹🇴', 'tt': '🇹🇹', 'tn': '🇹🇳', 'tr': '🇹🇷', 'tm': '🇹🇲',
  'tv': '🇹🇻', 'ug': '🇺🇬', 'ua': '🇺🇦', 'ae': '🇦🇪', 'gb': '🇬🇧',
  'us': '🇺🇸', 'uy': '🇺🇾', 'uz': '🇺🇿', 'vu': '🇻🇺', 'va': '🇻🇦',
  've': '🇻🇪', 'vn': '🇻🇳', 'ye': '🇾🇪', 'zm': '🇿🇲', 'zw': '🇿🇼',
  'other': '🌍'
};
