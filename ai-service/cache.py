"""
Cache simples em memoria para evitar reprocessar a mesma transcricao.
Nao armazena a transcricao em texto puro como chave.
"""

from collections import OrderedDict
from copy import deepcopy
import hashlib
import time
from typing import Any


class TTLCache:
    def __init__(self, ttl_seconds: int, max_entries: int):
        self.ttl_seconds = max(1, ttl_seconds)
        self.max_entries = max(1, max_entries)
        self._items: OrderedDict[str, tuple[float, dict[str, Any]]] = OrderedDict()

    def _key(self, text: str) -> str:
        return hashlib.sha256(text.encode("utf-8")).hexdigest()

    def get(self, text: str) -> dict[str, Any] | None:
        key = self._key(text)
        item = self._items.get(key)
        if not item:
            return None

        created_at, value = item
        if time.time() - created_at > self.ttl_seconds:
            self._items.pop(key, None)
            return None

        self._items.move_to_end(key)
        return deepcopy(value)

    def set(self, text: str, value: dict[str, Any]) -> None:
        key = self._key(text)
        self._items[key] = (time.time(), deepcopy(value))
        self._items.move_to_end(key)

        while len(self._items) > self.max_entries:
            self._items.popitem(last=False)

    def stats(self) -> dict[str, int]:
        return {
            "entries": len(self._items),
            "max_entries": self.max_entries,
            "ttl_seconds": self.ttl_seconds,
        }
