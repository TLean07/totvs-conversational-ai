"use client";

import Link from "next/link";

interface StatsCardProps {
  title: string;
  value: number | string;
  icon: React.ReactNode;
  color: "blue" | "green" | "red" | "yellow" | "purple";
  subtitle?: string;
  href?: string;
}

const colorMap = {
  blue: {
    accent: "bg-blue-500",
    icon: "bg-blue-50 text-blue-600 ring-blue-100",
    value: "text-blue-700",
    hover: "hover:border-blue-200",
  },
  green: {
    accent: "bg-emerald-500",
    icon: "bg-emerald-50 text-emerald-600 ring-emerald-100",
    value: "text-emerald-700",
    hover: "hover:border-emerald-200",
  },
  red: {
    accent: "bg-rose-500",
    icon: "bg-rose-50 text-rose-600 ring-rose-100",
    value: "text-rose-700",
    hover: "hover:border-rose-200",
  },
  yellow: {
    accent: "bg-amber-500",
    icon: "bg-amber-50 text-amber-600 ring-amber-100",
    value: "text-amber-700",
    hover: "hover:border-amber-200",
  },
  purple: {
    accent: "bg-violet-500",
    icon: "bg-violet-50 text-violet-600 ring-violet-100",
    value: "text-violet-700",
    hover: "hover:border-violet-200",
  },
};

export default function StatsCard({ title, value, icon, color, subtitle, href }: StatsCardProps) {
  const colors = colorMap[color];
  const inner = (
    <>
      <div className={`absolute left-0 top-0 h-full w-1.5 ${colors.accent}`} />
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="text-sm font-medium text-slate-500">{title}</p>
          <p className={`mt-2 text-3xl font-bold tracking-tight ${colors.value}`}>{value}</p>
          {subtitle && <p className="mt-2 text-xs font-medium text-slate-500">{subtitle}</p>}
        </div>
        <div className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl ring-1 ${colors.icon}`}>
          {icon}
        </div>
      </div>
    </>
  );

  if (href) {
    return (
      <Link
        href={href}
        aria-label={`Abrir ${title}`}
        className={`group relative block overflow-hidden rounded-2xl border border-slate-200 bg-white p-5 shadow-sm shadow-slate-200/60 transition-all duration-200 ${colors.hover} hover:-translate-y-0.5 hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2`}
      >
        {inner}
      </Link>
    );
  }

  return (
    <div className={`relative overflow-hidden rounded-2xl border border-slate-200 bg-white p-5 shadow-sm shadow-slate-200/60 transition-all duration-200 ${colors.hover}`}>
      {inner}
    </div>
  );
}
