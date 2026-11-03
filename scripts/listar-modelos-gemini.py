import os
import json
import urllib.request
import urllib.error

def carregar_env():
    env = {}
    if os.path.exists(".env"):
        with open(".env", "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    k, v = line.split("=", 1)
                    env[k.strip()] = v.strip().strip('"').strip("'")
    return env

def listar_modelos():
    env = carregar_env()
    chave = env.get("GEMINI_API_KEY")
    url = f"https://generativelanguage.googleapis.com/v1beta/models?key={chave}"

    req = urllib.request.Request(url)
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            corpo = resp.read().decode("utf-8")
            dados = json.loads(corpo)
            modelos = dados.get("models", [])
            print(f"[INFO] Total de modelos encontrados: {len(modelos)}")
            for m in modelos:
                name = m.get("name")
                methods = m.get("supportedGenerationMethods", [])
                if "generateContent" in methods:
                    print(f"- {name} (supported: {methods})")
    except urllib.error.HTTPError as e:
        print(f"[ERRO HTTP {e.code}] {e.read().decode('utf-8')}")
    except Exception as e:
        print(f"[ERRO] {e}")

if __name__ == "__main__":
    listar_modelos()
