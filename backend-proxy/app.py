"""
Minimal TMDB proxy for StarGallery (Flask).

Why: an API key compiled into an APK can be extracted. With this proxy the key lives only on
your server (environment variable) and the app talks to YOUR URL.

Run locally:
    pip install -r requirements.txt
    export TMDB_READ_TOKEN="<your TMDB API Read Access Token>"   # never commit this
    flask --app app run --port 8080

In the app set, in TmdbConfig.kt:
    BACKEND_PROXY_URL = "https://your-domain.example/3/"

Deploy behind HTTPS (Render, Fly.io, Railway, a VPS with nginx, ...). Do not enable debug mode.
"""
import os
import re
import time
from collections import defaultdict, deque

import requests
from flask import Flask, Response, abort, request

TMDB_BASE = "https://api.themoviedb.org/3/"
TOKEN = os.environ.get("TMDB_READ_TOKEN", "")

# Only the endpoints StarGallery uses. Everything else is rejected.
ALLOWED = [
    re.compile(r"^person/popular$"),
    re.compile(r"^trending/person/(day|week)$"),
    re.compile(r"^search/person$"),
    re.compile(r"^person/\d+$"),
    re.compile(r"^person/\d+/images$"),
    re.compile(r"^person/\d+/movie_credits$"),
    re.compile(r"^discover/movie$"),
    re.compile(r"^movie/\d+/credits$"),
]

RATE_LIMIT = 120          # requests ...
RATE_WINDOW = 60          # ... per this many seconds, per client IP
CACHE_TTL = 300           # seconds to cache identical GETs

app = Flask(__name__)
_hits = defaultdict(deque)
_cache = {}


def _client_ip():
    # Behind a reverse proxy, configure it to set X-Forwarded-For and trust only that proxy.
    return request.headers.get("X-Forwarded-For", request.remote_addr or "?").split(",")[0].strip()


def _rate_limited(ip):
    now = time.time()
    q = _hits[ip]
    while q and now - q[0] > RATE_WINDOW:
        q.popleft()
    if len(q) >= RATE_LIMIT:
        return True
    q.append(now)
    return False


@app.get("/3/<path:endpoint>")
def proxy(endpoint):
    if not TOKEN:
        abort(503, "Proxy is not configured")
    if not any(p.match(endpoint) for p in ALLOWED):
        abort(404)
    if _rate_limited(_client_ip()):
        abort(429)

    params = {k: v for k, v in request.args.items() if k != "api_key"}  # never forward client keys
    cache_key = (endpoint, tuple(sorted(params.items())))
    hit = _cache.get(cache_key)
    if hit and time.time() - hit[0] < CACHE_TTL:
        return Response(hit[1], status=200, mimetype="application/json")

    try:
        upstream = requests.get(
            TMDB_BASE + endpoint,
            params=params,
            headers={"Authorization": f"Bearer {TOKEN}", "Accept": "application/json"},
            timeout=10,
        )
    except requests.RequestException:
        abort(502)

    if upstream.status_code == 200:
        _cache[cache_key] = (time.time(), upstream.content)
        if len(_cache) > 2000:
            _cache.clear()
    return Response(upstream.content, status=upstream.status_code, mimetype="application/json")


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=8080)
