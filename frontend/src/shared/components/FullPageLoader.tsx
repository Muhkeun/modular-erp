export default function FullPageLoader() {
  return (
    <div className="flex items-center justify-center h-64 animate-in fade-in duration-300">
      <div className="flex flex-col items-center gap-4">
        <div className="animate-spin rounded-full h-8 w-8 border-2 border-slate-200 border-t-brand-600" />
        <span className="text-sm text-slate-400">Loading...</span>
      </div>
    </div>
  );
}
