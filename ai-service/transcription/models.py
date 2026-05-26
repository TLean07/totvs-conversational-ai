from dataclasses import dataclass, field


@dataclass(frozen=True)
class TranscriptionSegment:
    start: float | None
    end: float | None
    text: str
    confidence: float | None = None

    def to_dict(self) -> dict:
        return {
            "start": self.start,
            "end": self.end,
            "text": self.text,
            "confidence": self.confidence,
        }


@dataclass(frozen=True)
class TranscriptionResult:
    transcript: str
    language: str
    duration_seconds: float | None
    model: str
    processing_time_ms: int
    segments: list[TranscriptionSegment] = field(default_factory=list)
    confidence: float | None = None
    warnings: list[str] = field(default_factory=list)

    def to_dict(self) -> dict:
        return {
            "transcript": self.transcript,
            "language": self.language,
            "durationSeconds": self.duration_seconds,
            "model": self.model,
            "processingTimeMs": self.processing_time_ms,
            "segments": [segment.to_dict() for segment in self.segments],
            "confidence": self.confidence,
            "warnings": self.warnings,
        }


class TranscriptionError(Exception):
    def __init__(self, error_code: str, message: str, details: str | None = None, status_code: int = 503):
        super().__init__(message)
        self.error_code = error_code
        self.message = message
        self.details = details
        self.status_code = status_code

    def to_dict(self) -> dict:
        return {
            "errorCode": self.error_code,
            "message": self.message,
            "details": self.details,
            "fallbackAvailable": True,
        }
