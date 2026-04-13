"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const links = [
  { href: "/", label: "Dashboard" },
  { href: "/transcricoes", label: "Transcricoes" },
  { href: "/alertas", label: "Alertas" },
];

export default function Navbar() {
  const pathname = usePathname();

  return (
    <nav className="bg-[#1d4a7a] text-white shadow-lg">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-[#4CAF50] rounded-lg flex items-center justify-center font-bold text-sm">
              CI
            </div>
            <span className="font-bold text-lg">
              TOTVS Conversational Intelligence
            </span>
          </div>
          <div className="flex gap-1">
            {links.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                  pathname === link.href
                    ? "bg-white/20"
                    : "hover:bg-white/10"
                }`}
              >
                {link.label}
              </Link>
            ))}
          </div>
        </div>
      </div>
    </nav>
  );
}
