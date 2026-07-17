import { useNavigate } from 'react-router-dom';

export default function Header({ title, subtitle, back = false, right = null }) {
  const navigate = useNavigate();
  return (
    <header className="safe-top sticky top-0 z-10 border-b border-slate-200 bg-white/95 backdrop-blur">
      <div className="flex items-center gap-2 px-4 py-3">
        {back && (
          <button
            onClick={() => navigate(-1)}
            className="-ml-2 flex h-9 w-9 items-center justify-center rounded-full text-slate-600 hover:bg-slate-100"
            aria-label="Retour"
          >
            <span className="text-xl leading-none">‹</span>
          </button>
        )}
        <div className="min-w-0 flex-1">
          <h1 className="truncate text-base font-bold text-slate-800">{title}</h1>
          {subtitle && <p className="truncate text-xs text-slate-500">{subtitle}</p>}
        </div>
        {right}
      </div>
    </header>
  );
}
