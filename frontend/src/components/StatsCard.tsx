"use client";

interface StatsCardProps {
  title: string;
  value: number | string;
  icon: React.ReactNode;
  color: "blue" | "green" | "red" | "yellow" | "purple";
  subtitle?: string;
}

const colorMap = {
  blue: {
    bg: "bg-gradient-to-br from-blue-500 to-blue-600",
    icon: "bg-blue-400/30",
  },
  green: {
    bg: "bg-gradient-to-br from-emerald-500 to-emerald-600",
    icon: "bg-emerald-400/30",
  },
  red: {
    bg: "bg-gradient-to-br from-rose-500 to-rose-600",
    icon: "bg-rose-400/30",
  },
  yellow: {
    bg: "bg-gradient-to-br from-amber-500 to-amber-600",
    icon: "bg-amber-400/30",
  },
  purple: {
    bg: "bg-gradient-to-br from-violet-500 to-violet-600",
    icon: "bg-violet-400/30",
  },
};

export default function StatsCard({ title, value, icon, color, subtitle }: StatsCardProps) {
  const colors = colorMap[color];
  return (
    <div className={`${colors.bg} rounded-2xl p-5 text-white shadow-lg hover:shadow-xl transition-all duration-300 hover:-translate-y-0.5`}>
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-white/80">{title}</p>
          <p className="text-3xl font-bold mt-1 tracking-tight">{value}</p>
          {subtitle && <p className="text-xs text-white/60 mt-1">{subtitle}</p>}
        </div>
        <div className={`w-12 h-12 ${colors.icon} rounded-xl flex items-center justify-center`}>
          {icon}
        </div>
      </div>
    </div>
  );
}
