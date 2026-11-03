import os
import sys
import json
import urllib.request
import urllib.error

def main():
    print("[1] Lendo .env...", flush=True)
    chave = None
    with open(".env", "r", encoding="utf-8") as f:
        for line in f:
            if line.startswith("GEMINI_API_KEY="):
                chave = line.split("=", 1)[1].strip().strip('"').strip("'")
                break
    
    if not chave:
        print("[ERRO] Chave não encontrada!", flush=True)
        return

    print(f"[2] Chave encontrada: {chave[:8]}...{chave[-4:]}", flush=True)

    # Testar gemini-3.5-flash-lite
    modelo = "gemini-3.5-flash-lite"
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{modelo}:generateContent?key={chave}"
    
    payload = {
        "contents": [{
            "parts": [{
                "text": "Você é o assistente virtual da Chácara Recanto Verde. Um cliente disse: 'Bom dia!'. Responda cordialmente em uma frase curta."
            }]
        }]
    }

    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"}
    )

    print(f"[3] Enviando POST para {modelo}...", flush=True)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            data = resp.read().decode("utf-8")
            obj = json.loads(data)
            texto = obj["candidates"][0]["content"]["parts"][0]["text"]
            print(f"[SUCESSO HTTP {resp.status}]!", flush=True)
            print(f"RESPOSTA REAL DO GEMINI:\n{texto.strip()}", flush=True)
    except urllib.error.HTTPError as e:
        print(f"[HTTP ERROR {e.code}]: {e.read().decode('utf-8')}", flush=True)
    except Exception as e:
        print(f"[ERRO]: {e}", flush=True)

if __name__ == "__main__":
    main()
