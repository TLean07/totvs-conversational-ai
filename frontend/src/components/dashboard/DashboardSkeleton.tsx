"use client";

import Navbar from "@/components/Navbar";

function SkeletonBlock({ className = "" }: { className?: string }) {
  return <div className={`animate-pulse rounded-2xl bg-slate-200/70 ${className}`} />;
}

export default function DashboardSkeleton() {
  return (
    <>
      <Navbar />
      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <SkeletonBlock className="mb-8 h-56 rounded-3xl" />
        <div className="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
          {Array.from({ length: 5 }).map((_, index) => (
            <SkeletonBlock key={index} className="h-36" />
          ))}
        </div>
        <div className="mb-8 grid grid-cols-1 gap-6 lg:grid-cols-2">
          <SkeletonBlock className="h-96" />
          <SkeletonBlock className="h-96" />
        </div>
        <SkeletonBlock className="mb-8 h-72" />
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <SkeletonBlock className="h-96" />
          <SkeletonBlock className="h-96" />
        </div>
      </main>
    </>
  );
}
