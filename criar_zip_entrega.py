#!/usr/bin/env python3
"""Cria o zip de entrega do CodeRunner para o Moodle.

Inclui o código e o wrapper do Gradle (gradlew e gradle/wrapper).
Exclui a cache .gradle, as pastas build e o que o RunServer volta a criar.
"""

import zipfile
from pathlib import Path

RAIZ = Path(__file__).resolve().parent
PROJETO = RAIZ / "CodeRunner_TP1"
SAIDA = RAIZ / "CodeRunner_TP1_entrega.zip"

# Nomes de pastas que não vão no zip, em qualquer profundidade.
PASTAS_EXCLUIDAS = {".gradle", "build", "requests", ".idea", "bin", "__pycache__"}
SUFIXOS_EXCLUIDOS = {".class", ".iml"}


def excluir(relativo: Path) -> bool:
    if set(relativo.parts) & PASTAS_EXCLUIDAS:
        return True
    return relativo.suffix in SUFIXOS_EXCLUIDOS


def main() -> None:
    if not PROJETO.is_dir():
        raise SystemExit(f"Não encontrei o projeto em {PROJETO}")

    if SAIDA.exists():
        SAIDA.unlink()

    incluidos = 0
    with zipfile.ZipFile(SAIDA, "w", compression=zipfile.ZIP_DEFLATED) as arquivo:
        for ficheiro in sorted(PROJETO.rglob("*")):
            if not ficheiro.is_file():
                continue
            relativo = ficheiro.relative_to(RAIZ)
            if excluir(relativo):
                continue
            arquivo.write(ficheiro, relativo.as_posix())
            incluidos += 1

    print(f"Criado {SAIDA}")
    print(f"Ficheiros incluídos: {incluidos}")


if __name__ == "__main__":
    main()
