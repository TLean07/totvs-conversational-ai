# -*- coding: utf-8 -*-
"""Gera o script DDL (Oracle 19c) a partir do modelo."""
from modelo import TABELAS, SEQUENCIAS, TRIGGERS, EQUIPE, SOLUCAO, INTEGRANTES, tabela

CABECALHO = """-- =====================================================================================
--  {solucao}
--  Script DDL - Estrutura do banco de dados (Oracle Database 19c)
--
--  Equipe: {equipe}
{integrantes}
--
--  Challenge FIAP x TOTVS 2026 - Database Design - Sprint 3
--
--  Conteudo: sequence, tabelas (CREATE TABLE), constraints (PK, FK, UNIQUE, CHECK, NOT NULL),
--            coluna IDENTITY, indices, triggers de historico e COMMENT em todas as tabelas
--            e colunas.
--
--  Como executar: SQL Developer (abrir o arquivo e rodar como script, F5) ou
--                 SQLcl: sql usuario/senha@host:1521/servico @01_DDL_{arquivo}.sql
--  Ordem: as tabelas sao criadas do pai para o filho; a FK circular entre TRANSCRICOES e
--         ANALISES (1:1) e adicionada com ALTER TABLE ao final da secao de tabelas.
--  Para recriar do zero, execute antes a secao "LIMPEZA" (comentada) no fim do arquivo.
-- =====================================================================================
"""


def _integrantes():
    return "\n".join("--     %s - %s" % (n, rm) for n, rm in INTEGRANTES)


def _tipo(c):
    if c["identity"]:
        return "NUMBER(19,0) GENERATED ALWAYS AS IDENTITY"
    return c["tipo"]


def _linha_coluna(c, largura):
    partes = [c["nome"].ljust(largura), _tipo(c)]
    if c["default"] and not c["identity"]:
        partes.append("DEFAULT " + c["default"])
    if not c["nulo"] and not c["identity"]:
        partes.append("NOT NULL")
    if c["check"]:
        partes.append("CHECK (%s)" % c["check"])
    return "    " + " ".join(partes)


def create_table(t):
    largura = max(len(c["nome"]) for c in t["colunas"]) + 2
    linhas = [_linha_coluna(c, largura) for c in t["colunas"]]
    linhas.append("    CONSTRAINT %s PRIMARY KEY (%s)" % (t["pk"]["nome"], ", ".join(t["pk"]["colunas"])))
    for uk in t["uks"]:
        linhas.append("    CONSTRAINT %s UNIQUE (%s)" % (uk["nome"], ", ".join(uk["colunas"])))
    for fk in t["fks"]:
        if fk.get("posterior"):
            continue
        acao = (" ON DELETE " + fk["on_delete"]) if fk["on_delete"] else ""
        linhas.append("    CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)%s" % (
            fk["nome"], ", ".join(fk["colunas"]), fk["ref"], ", ".join(fk["ref_colunas"]), acao))
    for ck in t["checks"]:
        linhas.append("    CONSTRAINT %s CHECK (%s)" % (ck["nome"], ck["regra"]))
    return "CREATE TABLE %s (\n%s\n);\n" % (t["nome"], ",\n".join(linhas))


def comentarios(t):
    out = ["COMMENT ON TABLE  %s IS '%s';" % (t["nome"], t["comentario"].replace("'", "''"))]
    for c in t["colunas"]:
        out.append("COMMENT ON COLUMN %s.%s IS '%s';" % (t["nome"], c["nome"], c["comentario"].replace("'", "''")))
    return "\n".join(out) + "\n"


def indices(t):
    return "".join("CREATE INDEX %s ON %s (%s);\n" % (i["nome"], t["nome"], ", ".join(i["colunas"])) for i in t["indices"])


def gerar(arquivo_base="COMPANY404_TOTVS"):
    partes = [CABECALHO.format(solucao=SOLUCAO, equipe=EQUIPE, integrantes=_integrantes(), arquivo=arquivo_base)]

    partes.append("""
-- =====================================================================================
--  1. SEQUENCES
--  Os identificadores principais (ID) sao UUIDs gerados pela aplicacao (compatibilidade
--  com o backend e com a migracao do MongoDB). A sequence abaixo da um numero de
--  protocolo legivel e sequencial para a transcricao; a coluna recebe o valor por DEFAULT.
--  As tabelas de historico usam coluna IDENTITY (autonumeracao nativa do Oracle 12c+).
-- =====================================================================================
""")
    for s in SEQUENCIAS:
        partes.append("-- %s\nCREATE SEQUENCE %s START WITH %d INCREMENT BY 1 NOCACHE NOCYCLE;\n\n" % (
            s["comentario"], s["nome"], s["start"]))

    partes.append("""
-- =====================================================================================
--  2. TABELAS, CONSTRAINTS, INDICES E COMENTARIOS
-- =====================================================================================
""")
    grupo_atual = None
    for t in TABELAS:
        if t["grupo"] != grupo_atual:
            grupo_atual = t["grupo"]
            partes.append("\n-- -------------------------------------------------------------------------------------\n-- %s\n-- -------------------------------------------------------------------------------------\n" % grupo_atual.upper())
        partes.append("\n-- %s\n" % t["comentario"])
        partes.append(create_table(t))
        idx = indices(t)
        if idx:
            partes.append("\n" + idx)
        partes.append("\n" + comentarios(t))

    partes.append("""
-- -------------------------------------------------------------------------------------
-- FK CIRCULAR (1:1): a transcricao aponta para a sua analise atual e a analise aponta
-- para a transcricao. A restricao so pode ser criada depois que as duas tabelas existem.
-- ON DELETE SET NULL: ao apagar a analise, a transcricao continua existindo sem analise.
-- -------------------------------------------------------------------------------------
""")
    for t in TABELAS:
        for fk in t["fks"]:
            if fk.get("posterior"):
                acao = (" ON DELETE " + fk["on_delete"]) if fk["on_delete"] else ""
                partes.append("ALTER TABLE %s ADD CONSTRAINT %s\n    FOREIGN KEY (%s) REFERENCES %s (%s)%s;\n" % (
                    t["nome"], fk["nome"], ", ".join(fk["colunas"]), fk["ref"], ", ".join(fk["ref_colunas"]), acao))

    partes.append("""
-- =====================================================================================
--  3. TRIGGERS DE HISTORICO
--  Toda mudanca de STATUS em TRANSCRICOES e ALERTAS gera uma linha nas tabelas de
--  historico (auditoria da evolucao do processo). O ID do historico vem de IDENTITY.
-- =====================================================================================
""")
    for tg in TRIGGERS:
        partes.append("-- %s\n%s\n\n" % (tg["comentario"], tg["corpo"]))

    partes.append("""
-- =====================================================================================
--  4. CONFERENCIA
-- =====================================================================================
SELECT object_type, COUNT(*) AS quantidade
  FROM user_objects
 WHERE object_type IN ('TABLE', 'SEQUENCE', 'TRIGGER', 'INDEX')
 GROUP BY object_type
 ORDER BY object_type;

SELECT table_name, comments FROM user_tab_comments WHERE comments IS NOT NULL ORDER BY table_name;

/* =====================================================================================
   LIMPEZA (opcional) - remove todos os objetos deste script para recriar do zero.
   Descomente e execute antes do DDL caso o schema ja contenha as tabelas.
""")
    for t in reversed(TABELAS):
        partes.append("   DROP TABLE %s CASCADE CONSTRAINTS PURGE;\n" % t["nome"])
    for s in SEQUENCIAS:
        partes.append("   DROP SEQUENCE %s;\n" % s["nome"])
    partes.append("   ===================================================================================== */\n")
    return "".join(partes)


if __name__ == "__main__":
    print(gerar())
