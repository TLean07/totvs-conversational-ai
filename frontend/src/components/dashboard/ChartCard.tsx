"use client";

interface ChartCardProps {
  title: string;
  description: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
  className?: string;
}

export default function ChartCard({ title, description, children, footer, className = "" }: ChartCardProps) {
  return (
    <section className={`rounded-2xl border border-slate-200 bg-white p-5 shadow-sm shadow-slate-200/60 ${className}`}>
      <div className="mb-4 flex items-start justify-between gap-4">
        <div>
          <h2 className="text-base font-semibold text-slate-900">{title}</h2>
          <p className="mt-1 text-sm text-slate-500">{description}</p>
        </div>
      </div>
      {children}
      {footer && <div className="mt-4">{footer}</div>}
    </section>
  );
}
