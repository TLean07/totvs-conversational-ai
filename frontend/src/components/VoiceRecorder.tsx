"use client";

import { useState, useRef, useEffect, useCallback } from "react";

interface VoiceRecorderProps {
  onTranscript: (text: string) => void;
  currentText: string;
}

export default function VoiceRecorder({ onTranscript, currentText }: VoiceRecorderProps) {
  const [isRecording, setIsRecording] = useState(false);
  const [isSupported, setIsSupported] = useState(true);
  const [interim, setInterim] = useState("");
  const [seconds, setSeconds] = useState(0);
  const recognitionRef = useRef<SpeechRecognition | null>(null);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
      setIsSupported(false);
      return;
    }

    const recognition = new SpeechRecognition();
    recognition.lang = "pt-BR";
    recognition.continuous = true;
    recognition.interimResults = true;

    recognition.onresult = (event: SpeechRecognitionEvent) => {
      let finalTranscript = "";
      let interimTranscript = "";

      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript;
        if (event.results[i].isFinal) {
          finalTranscript += transcript + " ";
        } else {
          interimTranscript += transcript;
        }
      }

      if (finalTranscript) {
        onTranscript(currentText + finalTranscript);
      }
      setInterim(interimTranscript);
    };

    recognition.onerror = (event: SpeechRecognitionErrorEvent) => {
      if (event.error !== "no-speech") {
        console.error("Speech recognition error:", event.error);
        stopRecording();
      }
    };

    recognition.onend = () => {
      if (isRecording) {
        try {
          recognition.start();
        } catch {
          // ignore - may already be started
        }
      }
    };

    recognitionRef.current = recognition;

    return () => {
      recognition.abort();
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, []);

  // Update the onresult handler when currentText changes
  useEffect(() => {
    const recognition = recognitionRef.current;
    if (!recognition) return;

    recognition.onresult = (event: SpeechRecognitionEvent) => {
      let finalTranscript = "";
      let interimTranscript = "";

      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript;
        if (event.results[i].isFinal) {
          finalTranscript += transcript + " ";
        } else {
          interimTranscript += transcript;
        }
      }

      if (finalTranscript) {
        onTranscript(currentText + finalTranscript);
      }
      setInterim(interimTranscript);
    };
  }, [currentText, onTranscript]);

  const startRecording = useCallback(() => {
    if (!recognitionRef.current) return;
    setIsRecording(true);
    setSeconds(0);
    setInterim("");
    try {
      recognitionRef.current.start();
    } catch {
      // ignore
    }
    timerRef.current = setInterval(() => {
      setSeconds((s) => s + 1);
    }, 1000);
  }, []);

  const stopRecording = useCallback(() => {
    setIsRecording(false);
    setInterim("");
    if (recognitionRef.current) {
      try {
        recognitionRef.current.stop();
      } catch {
        // ignore
      }
    }
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
  }, []);

  const formatTime = (s: number) => {
    const min = Math.floor(s / 60);
    const sec = s % 60;
    return `${min.toString().padStart(2, "0")}:${sec.toString().padStart(2, "0")}`;
  };

  if (!isSupported) {
    return (
      <div className="flex items-center gap-2 text-xs text-slate-400 bg-slate-50 rounded-lg px-3 py-2">
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M12 18.75a6 6 0 006-6v-1.5m-6 7.5a6 6 0 01-6-6v-1.5m6 7.5v3.75m-3.75 0h7.5M12 15.75a3 3 0 01-3-3V4.5a3 3 0 116 0v8.25a3 3 0 01-3 3z" />
        </svg>
        Navegador nao suporta gravacao de voz. Use Chrome ou Edge.
      </div>
    );
  }

  return (
    <div className="flex items-center gap-3">
      {isRecording ? (
        <>
          <button
            type="button"
            onClick={stopRecording}
            className="flex items-center gap-2 px-4 py-2.5 bg-rose-500 text-white rounded-xl hover:bg-rose-600 transition-all duration-200 font-semibold text-sm shadow-lg shadow-rose-500/25"
          >
            <div className="w-3 h-3 bg-white rounded-sm" />
            Parar Gravacao
          </button>
          <div className="flex items-center gap-2 text-sm text-slate-500">
            <div className="w-2.5 h-2.5 bg-rose-500 rounded-full animate-pulse" />
            <span className="font-mono text-rose-600 font-medium">{formatTime(seconds)}</span>
            <span className="text-slate-400">Gravando...</span>
          </div>
          {interim && (
            <span className="text-xs text-slate-400 italic truncate max-w-xs">
              {interim}
            </span>
          )}
        </>
      ) : (
        <button
          type="button"
          onClick={startRecording}
          className="flex items-center gap-2 px-4 py-2.5 bg-gradient-to-r from-rose-500 to-pink-500 text-white rounded-xl hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 font-semibold text-sm"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 18.75a6 6 0 006-6v-1.5m-6 7.5a6 6 0 01-6-6v-1.5m6 7.5v3.75m-3.75 0h7.5M12 15.75a3 3 0 01-3-3V4.5a3 3 0 116 0v8.25a3 3 0 01-3 3z" />
          </svg>
          Gravar Reuniao
        </button>
      )}
    </div>
  );
}
