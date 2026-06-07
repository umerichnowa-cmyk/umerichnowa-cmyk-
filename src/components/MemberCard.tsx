import React, { useRef, useState } from 'react';
import { Member, FLAG_EMOJIS, NATIONALITIES } from '../types';
import { QRCodeSVG } from 'qrcode.react';
import { Download, ShieldCheck, RotateCcw } from 'lucide-react';
import html2canvas from 'html2canvas';
import { jsPDF } from 'jspdf';
// @ts-ignore
import aeidcLogoImg from '../assets/images/aeidc_logo_1780795174700.png';

interface MemberCardProps {
  member: Member;
}

const AEIDCLogo: React.FC<{ className?: string }> = ({ className = "w-10 h-10" }) => (
  <img 
    src={aeidcLogoImg}
    alt="AEIDC Logo"
    className={className}
    crossOrigin="anonymous"
    referrerPolicy="no-referrer"
  />
);

const AEIDCSignature: React.FC<{ className?: string, s?: number }> = ({ className = "w-20", s = 1 }) => (
  <div className={`flex flex-col items-center justify-center font-sans ${className}`} style={s !== 1 ? { width: `${80 * s}px` } : undefined}>
    {/* Combined Stamp and Signature Container - Circular & Uniform dimensions */}
    <div 
      className="relative flex items-center justify-center select-none bg-white rounded-full p-0.5 border border-[#b8860b]/40 shadow-md"
      style={{
        width: `${48 * s}px`,
        height: `${48 * s}px`,
        borderWidth: `${1 * s}px`,
        padding: `${2 * s}px`
      }}
    >
      {/* Official Circular Stamp - Replicated from user image */}
      <svg viewBox="0 0 200 200" className="absolute inset-0 w-full h-full opacity-80" fill="none" xmlns="http://www.w3.org/2000/svg">
        {/* Concentric stamp circles in blue-black stamp ink */}
        <circle cx="100" cy="100" r="92" stroke="#1d4ed8" strokeWidth="2.5" />
        <circle cx="100" cy="100" r="84" stroke="#1d4ed8" strokeWidth="1.2" />
        <circle cx="100" cy="100" r="54" stroke="#1d4ed8" strokeWidth="1" strokeDasharray="3 3" />
        
        {/* Curving text inside the stamp "ACADEMIE DES EXPERTS EN INGENIERIE" (top) */}
        <path id="stampTextTopPath" d="M 22 100 A 78 78 0 1 1 178 100" fill="none" />
        <text fontFamily="'Arial', sans-serif" fontWeight="900" fontSize="10.5" fill="#1d4ed8" letterSpacing="1.5">
          <textPath href="#stampTextTopPath" startOffset="50%" textAnchor="middle">
            * ACADEMIE DES EXPERTS *
          </textPath>
        </text>

        {/* Curving text inside the stamp "ET DECOUPAGES/DEVELOPPEMENT DES COMPETENCES" (bottom) */}
        <path id="stampTextBottomPath" d="M 178 100 A 78 78 0 0 1 22 100" fill="none" />
        <text fontFamily="'Arial', sans-serif" fontWeight="900" fontSize="10" fill="#1d4ed8" letterSpacing="1">
          <textPath href="#stampTextBottomPath" startOffset="50%" textAnchor="middle">
            DEVELOPPEMENT DES COMPETENCES
          </textPath>
        </text>

        {/* Laurels inside the stamp (left & right) representing academics */}
        {/* Left Laurel */}
        <path d="M 58 100 C 58 85, 65 72, 75 66" stroke="#1d4ed8" strokeWidth="1.5" strokeLinecap="round" />
        <circle cx="58" cy="95" r="2.5" fill="#1d4ed8" />
        <circle cx="61" cy="85" r="2.5" fill="#1d4ed8" />
        <circle cx="66" cy="76" r="2.5" fill="#1d4ed8" />
        <circle cx="72" cy="69" r="2.5" fill="#1d4ed8" />

        {/* Right Laurel */}
        <path d="M 142 100 C 142 85, 135 72, 125 66" stroke="#1d4ed8" strokeWidth="1.5" strokeLinecap="round" />
        <circle cx="142" cy="95" r="2.5" fill="#1d4ed8" />
        <circle cx="139" cy="85" r="2.5" fill="#1d4ed8" />
        <circle cx="134" cy="76" r="2.5" fill="#1d4ed8" />
        <circle cx="128" cy="69" r="2.5" fill="#1d4ed8" />

        {/* Central graduation cap inside the stamp */}
        <polygon points="100,75 118,81 100,87 82,81" fill="#1d4ed8" opacity="0.8" />
        <path d="M 88,83 L 88,87 C 88,92 112,92 112,87 L 112,83" fill="#1d4ed8" opacity="0.9" stroke="#ffffff" strokeWidth="0.5" />
        
        {/* Small Red Star at the very bottom center */}
        <path d="M100 144 L102 148 L107 149 L103 152 L104 157 L100 154 L96 157 L97 152 L93 149 L98 148 Z" fill="#b31f24" />
        
        {/* Star labels on the sides */}
        <text x="100" y="45" fontFamily="'Times New Roman', serif" fontWeight="bold" fontSize="11.5" fill="#1d4ed8" textAnchor="middle" letterSpacing="1">
          AEIDC
        </text>
        <text x="100" y="55" fontFamily="'Arial', sans-serif" fontWeight="900" fontSize="6.5" fill="#1d4ed8" textAnchor="middle" letterSpacing="2">
          PRESIDENCE
        </text>
      </svg>
      
      {/* Dr. Bakayoko's Calligraphy Signature Text (cursive styling) */}
      <p 
        className="absolute bottom-[24%] font-black text-[#1d4ed8]/40 leading-none tracking-tighter uppercase select-none z-10"
        style={{ fontSize: `${2.5 * s}px` }}
      >
        BAKAYOKO I.
      </p>

      {/* Official Signature Stroke - Handdrawn blue-ink curve representing the real signature from the uploaded stamp image */}
      <svg 
        viewBox="0 0 200 100" 
        className="absolute rotate-[-2deg] z-20 pointer-events-none" 
        fill="none" 
        xmlns="http://www.w3.org/2000/svg"
        style={{
          width: `${56 * s}px`,
          height: `${28 * s}px`
        }}
      >
        <path 
          d="M 25 50 Q 15 30, 30 25 T 50 65 Q 60 70, 75 30 T 95 60 Q 105 10, 135 15 C 150 12, 175 15, 185 30 C 190 35, 170 45, 135 50 C 95 55, 60 55, 35 48 C 25 45, 30 35, 55 33 C 80 31, 115 45, 155 52" 
          stroke="#0033cc" 
          strokeWidth="3.2" 
          strokeLinecap="round" 
          strokeLinejoin="round" 
        />
        {/* Vertical loops crossing and details */}
        <path d="M 75 30 Q 82 20, 85 28 Q 88 35, 82 45" stroke="#0033cc" strokeWidth="2.8" strokeLinecap="round" />
        <path d="M 125 15 Q 132 5, 138 12 Q 142 22, 130 32" stroke="#0033cc" strokeWidth="2.8" strokeLinecap="round" />
      </svg>
      
      {/* Semi-transparent text signature "Dr. Bakayoko Ibrahim" cursive overlay */}
      <p 
        className="absolute bottom-[2px] font-[Alex Brush] italic text-slate-800 font-semibold opacity-95 text-center select-none pointer-events-none" 
        style={{ fontFamily: '"Alex Brush", cursive', fontSize: `${6.5 * s}px` }}
      >
        Dr. Bakayoko Ibrahim
      </p>
    </div>

    {/* Black Separator Line from image */}
    <div 
      className="bg-slate-400 opacity-50"
      style={{
        width: `${80 * s}px`,
        height: `${1 * s}px`,
        marginTop: `${8 * s}px`,
        marginBottom: `${4 * s}px`
      }}
    ></div>
    {/* Identity Name and Title matching the stamp block exactly */}
    <p 
      className="font-bold text-white uppercase tracking-tight leading-none text-center"
      style={{ fontSize: `${6.5 * s}px` }}
    >
      Dr. BAKAYOKO Ibrahim, PhD
    </p>
    <p 
      className="text-[#d4af37] font-black uppercase tracking-wider leading-none mt-1 text-center font-sans"
      style={{ fontSize: `${5.5 * s}px`, marginTop: `${4 * s}px` }}
    >
      Président de l'AEIDC
    </p>
  </div>
);

function parsePercentOrFloat(val: string, maxVal = 1): number {
  if (val.endsWith('%')) {
    return (parseFloat(val) / 100) * maxVal;
  }
  return parseFloat(val);
}

function oklabToRgbRaw(L: number, a: number, b: number, alpha: number): string {
  // LMS representation
  const l_ = L + 0.3963377774 * a + 0.2158037573 * b;
  const m_ = L - 0.1055613458 * a - 0.0638541728 * b;
  const s_ = L - 0.0894841775 * a - 1.2914855480 * b;

  // Cube LMS to linear LMS
  const l = Math.pow(Math.max(0, l_), 3);
  const m = Math.pow(Math.max(0, m_), 3);
  const s = Math.pow(Math.max(0, s_), 3);

  // Linear sRGB conversion
  const r =  4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s;
  const g = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s;
  const bVal = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s;

  // Gamma correction to standard sRGB
  const gamma = (c: number) => {
    if (c <= 0.0031308) {
      return 12.92 * c;
    }
    return 1.055 * Math.pow(c, 1 / 2.4) - 0.055;
  };

  const r255 = Math.min(255, Math.max(0, Math.round(gamma(r) * 255)));
  const g255 = Math.min(255, Math.max(0, Math.round(gamma(g) * 255)));
  const b255 = Math.min(255, Math.max(0, Math.round(gamma(bVal) * 255)));

  if (alpha === 1) {
    return `rgb(${r255}, ${g255}, ${b255})`;
  } else {
    return `rgba(${r255}, ${g255}, ${b255}, ${alpha})`;
  }
}

function oklchToRgb(lStr: string, cStr: string, hStr: string, aStr?: string): string {
  const L = parsePercentOrFloat(lStr, 1);
  const C = parseFloat(cStr);
  const H = parseFloat(hStr);
  const alpha = aStr ? parsePercentOrFloat(aStr, 1) : 1;

  const hRad = (H * Math.PI) / 180;
  const a = C * Math.cos(hRad);
  const b = C * Math.sin(hRad);

  return oklabToRgbRaw(L, a, b, alpha);
}

function oklabToRgb(lStr: string, aStrVal: string, bStrVal: string, aStr?: string): string {
  const L = parsePercentOrFloat(lStr, 1);
  const a = parseFloat(aStrVal);
  const b = parseFloat(bStrVal);
  const alpha = aStr ? parsePercentOrFloat(aStr, 1) : 1;
  return oklabToRgbRaw(L, a, b, alpha);
}

function cleanColorString(str: string): string {
  if (!str || typeof str !== 'string') return str;
  if (!str.toLowerCase().includes('oklch') && !str.toLowerCase().includes('oklab')) return str;

  const colorRegex = /(oklch|oklab)\(([^)]+)\)/gi;
  return str.replace(colorRegex, (match, type, inner) => {
    try {
      const normalized = inner.replace(/\//g, ' ');
      const parts = normalized.replace(/[\s,]+/g, ' ').trim().split(' ');
      if (parts.length >= 3) {
        const lStr = parts[0] === 'none' ? '0' : parts[0];
        const aOrCStr = parts[1] === 'none' ? '0' : parts[1];
        const bOrHStr = parts[2] === 'none' ? '0' : parts[2];
        const alphaStr = (parts[3] !== undefined && parts[3] !== 'none') ? parts[3] : undefined;
        
        if (type.toLowerCase() === 'oklch') {
          return oklchToRgb(lStr, aOrCStr, bOrHStr, alphaStr);
        } else {
          return oklabToRgb(lStr, aOrCStr, bOrHStr, alphaStr);
        }
      }
    } catch (e) {
      console.warn("Failed to parse color match:", match, e);
    }
    return 'rgb(148, 163, 184)';
  });
}

const cleanStylesheets = async (): Promise<() => void> => {
  const links = Array.from(document.querySelectorAll('link[rel="stylesheet"]')) as HTMLLinkElement[];
  const styles = Array.from(document.querySelectorAll('style')) as HTMLStyleElement[];
  
  const restorations: Array<{ element: HTMLElement; originalVal: string; type: 'link' | 'style' }> = [];

  // 1. Clean style tags
  for (const style of styles) {
    const originalText = style.innerHTML;
    if (originalText.includes('oklch') || originalText.includes('oklab')) {
      const css = cleanColorString(originalText);
      style.innerHTML = css;
      restorations.push({ element: style, originalVal: originalText, type: 'style' });
    }
  }

  // 2. Clean link tags
  for (const link of links) {
    try {
      if (link.href && (link.href.startsWith(window.location.origin) || link.href.startsWith('/'))) {
        const response = await fetch(link.href);
        if (response.ok) {
          const text = await response.text();
          if (text.includes('oklch') || text.includes('oklab')) {
            const css = cleanColorString(text);
            
            // Create a temporary style tag
            const tempStyle = document.createElement('style');
            tempStyle.innerHTML = css;
            tempStyle.id = `temp-clean-${Math.random().toString(36).substring(2, 9)}`;
            document.head.appendChild(tempStyle);
            
            // Disable original link
            link.disabled = true;
            
            restorations.push({ 
              element: link, 
              originalVal: tempStyle.id, 
              type: 'link' 
            });
          }
        }
      }
    } catch (e) {
      console.warn("Could not clean link element:", link.href, e);
    }
  }

  return () => {
    // Restore original styles
    for (const rest of restorations) {
      if (rest.type === 'style') {
        (rest.element as HTMLStyleElement).innerHTML = rest.originalVal;
      } else if (rest.type === 'link') {
        (rest.element as HTMLLinkElement).disabled = false;
        const tempStyle = document.getElementById(rest.originalVal);
        if (tempStyle) {
          tempStyle.remove();
        }
      }
    }
  };
};

export const MemberCard: React.FC<MemberCardProps> = ({ member }) => {
  const cardRef = useRef<HTMLDivElement>(null);
  const printRef = useRef<HTMLDivElement>(null);
  const printRectoRef = useRef<HTMLDivElement>(null);
  const printVersoRef = useRef<HTMLDivElement>(null);
  const [flipped, setFlipped] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const [flagError, setFlagError] = useState(false);

  const downloadPDF = async (formatType: 'A4' | 'CR80' = 'CR80') => {
    setIsExporting(true);
    
    let restoreStyles: (() => void) | null = null;
    try {
      restoreStyles = await cleanStylesheets();
    } catch (err) {
      console.warn("Stylesheet cleaning failed:", err);
    }

    // Give time for the hidden print element to be rendered
    setTimeout(async () => {
      const originalGetComputedStyle = window.getComputedStyle;
      try {
        // Override window.getComputedStyle to intercept "oklch" and "oklab" colors
        window.getComputedStyle = function(elt: Element, pseudo?: string | null) {
          const style = originalGetComputedStyle.call(this, elt, pseudo);
          return new Proxy(style, {
            get(target, prop) {
              const value = Reflect.get(target, prop);
              if (typeof value === 'function') {
                if (prop === 'getPropertyValue') {
                  return function(propertyName: string) {
                    const val = target.getPropertyValue(propertyName);
                    return cleanColorString(val);
                  };
                }
                return value.bind(target);
              }
              if (typeof value === 'string') {
                return cleanColorString(value);
              }
              return value;
            }
          }) as any;
        };

        const canvasOptions = {
          scale: 4,
          useCORS: true,
          backgroundColor: '#ffffff',
          logging: false,
          onclone: (clonedDoc: Document) => {
            // Remove <link> tags in cloned doc to prevent html2canvas attempting to parse raw files (with oklch/oklab)
            const linkTags = Array.from(clonedDoc.querySelectorAll('link[rel="stylesheet"]'));
            linkTags.forEach(link => {
              link.remove();
            });

            const styleTags = Array.from(clonedDoc.querySelectorAll('style'));
            styleTags.forEach(style => {
              if (style.innerHTML.toLowerCase().includes('oklch') || style.innerHTML.toLowerCase().includes('oklab')) {
                style.innerHTML = cleanColorString(style.innerHTML);
              }
            });

            // Clean inline style attributes in cloned doc
            const inlineStyles = Array.from(clonedDoc.querySelectorAll('[style]'));
            inlineStyles.forEach(elem => {
              const val = elem.getAttribute('style');
              if (val && (val.toLowerCase().includes('oklch') || val.toLowerCase().includes('oklab'))) {
                elem.setAttribute('style', cleanColorString(val));
              }
            });
          }
        };

        if (formatType === 'CR80') {
          if (!printRectoRef.current || !printVersoRef.current) return;
          
          const canvasRecto = await html2canvas(printRectoRef.current, canvasOptions);
          const canvasVerso = await html2canvas(printVersoRef.current, canvasOptions);
          
          const imgRecto = canvasRecto.toDataURL('image/png');
          const imgVerso = canvasVerso.toDataURL('image/png');
          
          const pdf = new jsPDF({
            orientation: 'landscape',
            unit: 'mm',
            format: [86, 54], // Exact ID Card specifications in CR80 standard format (86mm x 54mm)
          });

          // Page 1: Recto
          pdf.addImage(imgRecto, 'PNG', 0, 0, 86, 54);

          // Page 2: Verso
          pdf.addPage([86, 54], 'landscape');
          pdf.addImage(imgVerso, 'PNG', 0, 0, 86, 54);

          pdf.save(`Carte_AEIDC_${member.matricule}_CR80.pdf`);
        } else {
          if (!printRef.current) return;
          const canvas = await html2canvas(printRef.current, canvasOptions);
          const imgData = canvas.toDataURL('image/png');
          const pdf = new jsPDF({
            orientation: 'landscape',
            unit: 'mm',
            format: 'a4',
          });

          // 380px x 240px each. Total: 800px width (avec espacement de 40px)
          const pageWidth = 297;
          const pageHeight = 210;
          const imgWidth = 211.67;
          const imgHeight = 63.5;
          const x = (pageWidth - imgWidth) / 2;
          const y = (pageHeight - imgHeight) / 2;

          pdf.addImage(imgData, 'PNG', x, y, imgWidth, imgHeight);
          pdf.save(`Carte_AEIDC_${member.matricule}_A4_Impression.pdf`);
        }
      } catch (err) {
        console.error("PDF Export failed:", err);
      } finally {
        // Restore window.getComputedStyle
        window.getComputedStyle = originalGetComputedStyle;
        if (restoreStyles) {
          restoreStyles();
        }
        setIsExporting(false);
      }
    }, 100);
  };

  const getStatusDisplay = () => {
    if (member.memberType === 'ETUDIANT' && member.educationLevel) {
      return `Étudiant ${member.educationLevel}${member.specialization ? ` (${member.specialization})` : ''}`;
    }
    switch (member.memberType) {
      case 'EXPERT': return 'Expert AEIDC';
      case 'ACTIF': return 'Membre Actif';
      case 'PERSONNEL': return 'Personnel AEIDC';
      case 'STAGIAIRE': return 'Stagiaire AEIDC';
      case 'TUTEUR': return 'Tuteur AEIDC';
      default: return 'Étudiant';
    }
  };

  const getMemberTypeDisplay = () => {
    switch (member.memberType) {
      case 'EXPERT': return 'Expert AEIDC';
      case 'ACTIF': return 'Membre Actif';
      case 'PERSONNEL': return 'Personnel AEIDC';
      case 'STAGIAIRE': return 'Stagiaire AEIDC';
      case 'TUTEUR': return 'Tuteur AEIDC';
      default: return 'Étudiant';
    }
  };

  const formatDate = (timestamp: any) => {
    try {
      if (!timestamp) return '02/02/2026';
      if (timestamp.toDate) {
        const date = timestamp.toDate();
        return date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
      }
      if (timestamp.seconds) {
        const date = new Date(timestamp.seconds * 1000);
        return date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
      }
      return '02/02/2026';
    } catch (e) {
      return '02/02/2026';
    }
  };

  // Shared card design to ensure consistency with the new AEIDC identity
  const CardSide = ({ side, isPrint = false }: { side: 'recto' | 'verso', isPrint?: boolean }) => {
    const s = isPrint ? 3 : 1;
    return (
      <div 
        className="overflow-hidden relative font-sans" 
        style={{ 
          width: `${380 * s}px`,
          height: `${240 * s}px`,
          borderRadius: `${16 * s}px`,
          background: 'linear-gradient(145deg, #0a1929 0%, #1a365d 50%, #0d2137 100%)', 
          border: `${3 * s}px solid #b8860b`,
          boxShadow: isPrint ? 'none' : `inset 0 0 ${30 * s}px rgba(184, 134, 11, 0.15), 0 ${10 * s}px ${40 * s}px rgba(0,0,0,0.5)`
        }}
      >
        {/* Background Texture Pattern */}
        <div 
          className="absolute inset-0 opacity-[0.03] pointer-events-none" 
          style={{ 
            backgroundImage: 'url("data:image/svg+xml,%3Csvg width=\'60\' height=\'60\' viewBox=\'0 0 60 60\' xmlns=\'http://www.w3.org/2000/svg\'%3E%3Cg fill=\'none\' fill-rule=\'evenodd\'%3E%3Cg fill=\'%23b8860b\' fill-opacity=\'1\'%3E%3Cpath d=\'M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z\'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")',
            backgroundSize: `${60 * s}px ${60 * s}px`
          }}
        ></div>

        {side === 'recto' ? (
          <div className="h-full flex flex-col relative z-10" style={{ padding: `${12 * s}px` }}>
            {/* Header Section */}
            <div 
              className="flex justify-between items-center border-[#b8860b]/40" 
              style={{ paddingBottom: `${8 * s}px`, borderBottomWidth: `${2 * s}px`, marginBottom: `${8 * s}px` }}
            >
              {/* Rectangular Flag container matching the Rectangular Logo on the right */}
              <div 
                className="shrink-0 flex items-center justify-center overflow-hidden shadow-md relative"
                style={{
                  width: `${42 * s}px`,
                  height: `${28 * s}px`,
                  borderRadius: `${4 * s}px`,
                  border: `${1 * s}px solid rgba(184, 134, 11, 0.4)`,
                  backgroundColor: 'rgba(255, 255, 255, 0.05)'
                }}
              >
                {member.nationality.toLowerCase() === 'ci' ? (
                  <div className="w-full h-full flex" title="Côte d'Ivoire">
                    {/* Absolute pure vector bands of the Côte d'Ivoire colors (vibrant orange, white, and green) */}
                    <div className="w-1/3 h-full bg-[#f77f00]"></div>
                    <div className="w-1/3 h-full bg-[#ffffff]"></div>
                    <div className="w-1/3 h-full bg-[#009b3a]"></div>
                  </div>
                ) : (
                  <>
                    {!flagError ? (
                      <img 
                        src={`https://flagcdn.com/w80/${member.nationality.toLowerCase()}.png`} 
                        alt="" 
                        crossOrigin="anonymous" 
                        referrerPolicy="no-referrer"
                        className="w-full h-full object-cover" 
                        onError={() => {
                          setFlagError(true);
                        }}
                      />
                    ) : (
                      <span className="absolute pointer-events-none" style={{ fontSize: `${0.95 * s}rem` }}>{FLAG_EMOJIS[member.nationality] || '🌍'}</span>
                    )}
                  </>
                )}
              </div>
              
              <div className="text-center flex-1">
                 <h3 
                   className="font-bold text-[#d4af37] font-serif drop-shadow-md uppercase italic leading-none"
                   style={{ fontSize: `${1.42 * s}rem`, letterSpacing: `${4 * s}px`, marginBottom: `${2 * s}px` }}
                 >
                   AEIDC
                 </h3>
                 <p 
                   className="text-white font-bold uppercase tracking-wide leading-none"
                   style={{ fontSize: `${6.2 * s}px` }}
                 >
                   Académie Des Experts En Ingénierie Et Développement Des Compétences
                 </p>
              </div>
              
              {/* Rectangular Logo container matching the Rectangular Flag on the left */}
              <div 
                className="bg-white shrink-0 flex items-center justify-center overflow-hidden shadow-md"
                style={{
                  width: `${42 * s}px`,
                  height: `${28 * s}px`,
                  borderRadius: `${4 * s}px`,
                  border: `${1 * s}px solid rgba(184, 134, 11, 0.4)`,
                  padding: `${2 * s}px`
                }}
              >
                 <AEIDCLogo className="w-full h-full object-contain" />
              </div>
            </div>

            {/* Content Section */}
            <div className="flex flex-1" style={{ gap: `${12 * s}px`, paddingTop: `${4 * s}px` }}>
               <div className="relative shrink-0">
                  <div 
                    className="bg-[#1a365d] overflow-hidden shadow-lg"
                    style={{
                      width: `${80 * s}px`,
                      height: `${100 * s}px`,
                      borderRadius: `${8 * s}px`,
                      border: `${2 * s}px solid #d4af37`
                    }}
                  >
                    {member.photoUrl ? (
                      <img src={member.photoUrl} alt="" className="w-full h-full object-cover" />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center">
                         <ShieldCheck className="text-[#d4af37] opacity-20" style={{ width: `${40 * s}px`, height: `${40 * s}px` }} />
                      </div>
                    )}
                  </div>
                  <div 
                    className="absolute left-1/2 -translate-x-1/2 text-[#0a1929] font-bold uppercase shadow-md whitespace-nowrap text-center border border-white/20" 
                    style={{ 
                      bottom: `-${8 * s}px`,
                      padding: `${2.5 * s}px ${10 * s}px`,
                      fontSize: `${7.5 * s}px`,
                      minWidth: `${75 * s}px`,
                      borderRadius: '9999px',
                      background: 'linear-gradient(to right, #b8860b, #d4af37)' 
                    }}
                  >
                    {getMemberTypeDisplay().toUpperCase()}
                  </div>
               </div>

               <div className="flex-1 flex flex-col pt-1" style={{ gap: `${4 * s}px` }}>
                  <div 
                    className="border-l-4 border-[#d4af37] rounded-r-lg" 
                    style={{ 
                      backgroundColor: 'rgba(184, 134, 11, 0.15)',
                      borderLeftWidth: `${4 * s}px`,
                      padding: `${6 * s}px`
                    }}
                  >
                    <p 
                      className="text-[#d4af37] font-bold uppercase tracking-widest leading-none"
                      style={{ fontSize: `${6.5 * s}px`, marginBottom: `${4 * s}px` }}
                    >
                      NOM COMPLET
                    </p>
                    <p 
                      className="font-bold text-white leading-tight uppercase font-serif drop-shadow-sm"
                      style={{ fontSize: `${12.5 * s}px` }}
                    >
                      {member.fullName}
                    </p>
                  </div>
                  
                  <div className="flex flex-col" style={{ gap: `${4 * s}px` }}>
                    <div>
                       <p className="font-bold uppercase tracking-tight text-[#d4af37]" style={{ fontSize: `${6.5 * s}px` }}>MATRICULE</p>
                       <p className="font-mono font-bold text-white tracking-tighter" style={{ fontSize: `${10 * s}px` }}>{member.matricule}</p>
                    </div>
                    <div>
                       <p className="font-bold uppercase tracking-tight text-[#d4af37]" style={{ fontSize: `${6.5 * s}px` }}>NATIONALITÉ</p>
                       <p className="font-bold text-white uppercase italic whitespace-nowrap leading-tight" style={{ fontSize: `${10 * s}px` }}>{NATIONALITIES[member.nationality] || member.nationality}</p>
                    </div>
                    <div>
                       <p className="font-bold uppercase tracking-tight text-[#d4af37]" style={{ fontSize: `${6.5 * s}px` }}>FONCTION</p>
                       <p className="font-bold text-white italic whitespace-nowrap leading-tight" style={{ fontSize: `${10 * s}px` }}>{getStatusDisplay()}</p>
                    </div>
                  </div>

                  <div className="mt-auto">
                    <div 
                      className="inline-flex items-center text-white font-bold uppercase tracking-wider shadow-sm" 
                      style={{ 
                        background: 'linear-gradient(to right, #166534, #15803d)',
                        padding: `${2 * s}px ${8 * s}px`,
                        borderRadius: '9999px',
                        fontSize: `${7 * s}px`,
                        gap: `${4 * s}px`
                      }}
                    >
                       <ShieldCheck style={{ width: `${8 * s}px`, height: `${8 * s}px` }} />
                       VALIDE: {formatDate(member.createdAt)} - 31/12/2026
                    </div>
                  </div>
               </div>
            </div>
            
            <div 
              className="mt-2" 
              style={{ 
                height: `${4 * s}px`, 
                background: 'linear-gradient(to right, transparent, #d4af37, transparent)' 
              }}
            ></div>
          </div>
        ) : (
          <div className="h-full flex flex-col relative z-10 text-white w-full" style={{ padding: `${12 * s}px` }}>
            {/* Header Verso - Lifted slightly to free up space below */}
            <div 
              className="text-center border-b border-[#b8860b]/30 flex flex-col items-center relative z-20"
              style={{ 
                paddingBottom: `${1.5 * s}px`, 
                borderBottomWidth: `${1 * s}px`, 
                marginBottom: `${6 * s}px` 
              }}
            >
               {/* Rectangular top logo - as requested for matching rectangular shape on verso */}
               <div 
                 className="flex items-center justify-center border border-[#b8860b]/40 bg-white shadow-md animate-in fade-in zoom-in duration-300"
                 style={{
                   width: `${40 * s}px`,
                   height: `${22 * s}px`,
                   borderRadius: `${3 * s}px`,
                   borderWidth: `${1 * s}px`,
                   padding: `${1.5 * s}px`,
                   marginBottom: `${1.5 * s}px`,
                   marginTop: `-${4 * s}px`
                 }}
               >
                   <AEIDCLogo className="w-full h-full object-contain" />
               </div>
               <h3 
                 className="font-bold text-[#d4af37] font-serif leading-none uppercase italic"
                 style={{ 
                   fontSize: `${0.62 * s}rem`, 
                   letterSpacing: `${2 * s}px`, 
                   marginBottom: `${1.5 * s}px` 
                 }}
               >
                 CARTE DE MEMBRE
               </h3>
               <p 
                 className="text-white/80 font-bold uppercase tracking-wider leading-none"
                 style={{ fontSize: `${4.5 * s}px` }}
               >
                 AEIDC - Académie Des Experts En Ingénierie Et Développement Des Compétences
               </p>
            </div>

            <div className="flex-1 flex min-h-0" style={{ gap: `${12 * s}px` }}>
               {/* Left Side: Fields list */}
               <div className="flex-1 flex flex-col" style={{ gap: `${2.5 * s}px` }}>
                  {[
                    { label: 'N° Membre:', value: member.matricule },
                    { label: 'Email:', value: member.email },
                    { label: 'Fonction:', value: getStatusDisplay() },
                    { label: 'Expiration:', value: '31/12/2026' }
                  ].map((item, idx) => {
                    const valLength = item.value?.length || 0;
                    // Dynamic font size assignment based on value characters length
                    const valFontSize = valLength > 28 
                      ? `${5.2 * s}px` 
                      : valLength > 22 
                        ? `${5.8 * s}px` 
                        : valLength > 16 
                          ? `${6.4 * s}px` 
                          : `${7 * s}px`;

                    return (
                      <div 
                        key={idx} 
                        className="flex justify-between items-center" 
                        style={{ 
                          paddingTop: `${1.5 * s}px`,
                          paddingBottom: `${1.5 * s}px`,
                          borderBottom: `${1 * s}px solid rgba(184, 134, 11, 0.2)` 
                        }}
                      >
                         <span className="text-[#d4af37] font-bold whitespace-nowrap" style={{ fontSize: `${6.8 * s}px` }}>{item.label}</span>
                         <span 
                           className="text-white font-bold text-right" 
                           style={{ 
                             fontSize: valFontSize, 
                             maxWidth: `${215 * s}px`,
                             whiteSpace: 'nowrap',
                             overflow: 'visible'
                           }}
                         >
                           {item.value}
                         </span>
                      </div>
                    );
                  })}
                  <p className="text-white/95 font-semibold italic leading-tight" style={{ fontSize: `${5.8 * s}px`, marginTop: `${6 * s}px` }}>
                     Cette carte est strictement personnelle et incessible. Propriété de l'AEIDC.
                  </p>
               </div>

               {/* Right Side: Small photo & type designation */}
               <div 
                 className="flex flex-col items-center shrink-0 border-l border-[#b8860b]/20"
                 style={{ 
                   width: `${60 * s}px`, 
                   paddingLeft: `${8 * s}px`,
                   gap: `${4 * s}px`,
                   borderLeftWidth: `${1 * s}px`
                 }}
               >
                  <div 
                    className="bg-[#1a365d] overflow-hidden border border-[#d4af37] shadow-sm"
                    style={{
                      width: `${40 * s}px`,
                      height: `${50 * s}px`,
                      borderWidth: `${1 * s}px`,
                      borderRadius: `${4 * s}px`
                    }}
                  >
                     {member.photoUrl ? (
                       <img src={member.photoUrl} alt="" className="w-full h-full object-cover" />
                     ) : (
                       <div className="w-full h-full flex items-center justify-center">
                          <ShieldCheck className="text-[#d4af37] opacity-20" style={{ width: `${16 * s}px`, height: `${16 * s}px` }} />
                       </div>
                     )}
                  </div>
                  <div 
                    className="text-[#0a1628] font-bold rounded uppercase whitespace-nowrap overflow-hidden text-center w-full" 
                    style={{ 
                      background: 'linear-gradient(to right, #b8860b, #d4af37)',
                      padding: `${2 * s}px ${4 * s}px`,
                      fontSize: `${4.5 * s}px`,
                      borderRadius: `${4 * s}px`
                    }}
                  >
                     {getMemberTypeDisplay()}
                  </div>
               </div>
            </div>

            {/* Footer Verso */}
            <div 
              className="flex justify-between items-end mt-auto border-t border-[#b8860b]/20"
              style={{ 
                paddingTop: `${4 * s}px`,
                borderTopWidth: `${1 * s}px`
              }}
            >
               <div 
                 className="bg-white rounded shadow-sm flex items-center justify-center border border-[#e2e8f0]"
                 style={{
                   padding: `${2 * s}px`,
                   borderRadius: `${4 * s}px`
                 }}
               >
                  <QRCodeSVG value={member.matricule} size={30 * s} />
               </div>
               <AEIDCSignature s={s} />
            </div>
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="flex flex-col items-center gap-6 w-full">
      {/* Visual Workspace: Side-by-Side on Desktop, Interactive Flip on Mobile */}
      <div className="hidden md:flex flex-row flex-wrap justify-center items-center gap-10">
        <div className="flex flex-col items-center gap-2">
          <span className="text-[9px] text-[#d4af37] font-black uppercase tracking-[0.2em] bg-white/5 px-2.5 py-1 rounded-full border border-[#d4af37]/20">RECTO (Avant)</span>
          <CardSide side="recto" isPrint={false} />
        </div>
        <div className="flex flex-col items-center gap-2">
          <span className="text-[9px] text-[#d4af37] font-black uppercase tracking-[0.2em] bg-white/5 px-2.5 py-1 rounded-full border border-[#d4af37]/20">VERSO (Arrière)</span>
          <CardSide side="verso" isPrint={false} />
        </div>
      </div>

      <div className="md:hidden">
        {/* Interactive Display Card */}
        <div 
          className="group relative w-[380px] h-[240px] cursor-pointer"
          style={{ perspective: '1500px' }}
          onClick={() => setFlipped(!flipped)}
        >
          <div 
            className={`relative w-full h-full transition-transform duration-700 preserve-3d ${flipped ? 'rotate-y-180' : ''}`}
          >
            <div className="absolute inset-0 w-full h-full backface-hidden">
               <CardSide side="recto" isPrint={false} />
            </div>
            <div className="absolute inset-0 w-full h-full backface-hidden rotate-y-180">
               <CardSide side="verso" isPrint={false} />
            </div>
          </div>

          <div className="absolute -bottom-8 left-0 right-0 flex justify-center items-center gap-2 text-slate-500 text-[10px] font-bold uppercase tracking-wider animate-pulse">
             <RotateCcw className="w-3 h-3" />
             Cliquez pour retourner
          </div>
        </div>
      </div>

      {/* Hidden Side-by-Side Printable Element - Optimized for printing with refs for both separate cards and unified block */}
      <div className="fixed -left-[5000px] top-0 overflow-hidden" style={{ width: '1200px', height: '1000px', backgroundColor: '#ffffff' }}>
        <div ref={printRef} className="inline-flex bg-white items-start" style={{ border: 'none', padding: '0', margin: '0', gap: '40px' }}>
           <div ref={printRectoRef} className="bg-white" style={{ border: 'none', padding: '0', margin: '0' }}>
              <CardSide side="recto" isPrint={true} />
           </div>
           <div ref={printVersoRef} className="bg-white" style={{ border: 'none', padding: '0', margin: '0' }}>
              <CardSide side="verso" isPrint={true} />
           </div>
        </div>
      </div>

      <div className="flex flex-col sm:flex-row gap-4 mt-4 w-full justify-center max-w-xl">
        <button 
          disabled={isExporting}
          onClick={() => downloadPDF('CR80')}
          className="flex-1 flex items-center justify-center gap-2 px-6 py-3.5 bg-gradient-to-r from-emerald-600 to-green-500 text-white font-black rounded-xl hover:scale-105 transition-all text-sm shadow-xl shadow-green-950/10 disabled:opacity-50 cursor-pointer"
        >
          {isExporting ? "Génération..." : (
            <>
              <Download className="w-4 h-4" />
              Télécharger Format Carte ID (PDF CR80)
            </>
          )}
        </button>
        <button 
          disabled={isExporting}
          onClick={() => downloadPDF('A4')}
          className="flex-1 flex items-center justify-center gap-2 px-6 py-3.5 bg-gradient-to-r from-[#b8860b] to-[#d4af37] text-[#0a1628] font-black rounded-xl hover:scale-105 transition-all text-sm shadow-xl shadow-[#b8860b]/20 disabled:opacity-50 cursor-pointer"
        >
          {isExporting ? "Génération..." : (
            <>
              <Download className="w-4 h-4" />
              Télécharger Planche A4 (Impression)
            </>
          )}
        </button>
      </div>
      
      <p className="text-[10px] text-slate-400 font-bold uppercase max-w-lg text-center leading-relaxed">
        Le PDF est généré au format A4 paysage avec un espacement de 40 pixels (10mm env.) entre le recto et le verso afin de ne pas être collés, facilitant ainsi la découpe lors de l'impression.
      </p>
    </div>
  );
};
