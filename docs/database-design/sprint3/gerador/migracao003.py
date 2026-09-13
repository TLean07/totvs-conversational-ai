# -*- coding: utf-8 -*-
"""
Gera a migracao 003 para schemas ja existentes (Oracle da FIAP): objetos novos da Sprint 3
(sequence + NUM_PROTOCOLO, tabelas de historico, triggers) e COMMENT em todas as colunas.
"""
from modelo import TABELAS, TRIGGERS, tabela
from ddl import create_table, comentarios, indices

NOVAS = ["TRANSCRICAO_STATUS_HIST", "ALERTA_STATUS_HIST"]


def gerar():
    p = ["""-- =====================================================================================
--  Migracao 003 - Database Design Sprint 3 (aplicar em schemas criados com o schema.sql anterior)
--  * SEQ_TRANSCRICAO_PROTOCOLO + TRANSCRICOES.NUM_PROTOCOLO (numera as reunioes existentes por
--    ordem de criacao, depois vira DEFAULT NOT NULL UNIQUE)
--  * TRANSCRICAO_STATUS_HIST / ALERTA_STATUS_HIST (IDENTITY) + triggers de historico
--  * COMMENT ON em todas as tabelas e colunas
--  Idempotencia: nao e; rode uma unica vez. Para um schema novo use o schema.sql.
-- =====================================================================================

CREATE SEQUENCE SEQ_TRANSCRICAO_PROTOCOLO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

ALTER TABLE TRANSCRICOES ADD (NUM_PROTOCOLO NUMBER(10,0));

-- numera as reunioes ja cadastradas na ordem em que foram criadas
BEGIN
    FOR r IN (SELECT ID FROM TRANSCRICOES ORDER BY DATA_CRIACAO NULLS LAST, ID) LOOP
        UPDATE TRANSCRICOES SET NUM_PROTOCOLO = SEQ_TRANSCRICAO_PROTOCOLO.NEXTVAL WHERE ID = r.ID;
    END LOOP;
    COMMIT;
END;
/

ALTER TABLE TRANSCRICOES MODIFY (NUM_PROTOCOLO DEFAULT SEQ_TRANSCRICAO_PROTOCOLO.NEXTVAL NOT NULL);
ALTER TABLE TRANSCRICOES ADD CONSTRAINT UK_TRANSCRICOES_PROTOCOLO UNIQUE (NUM_PROTOCOLO);

"""]
    for nome in NOVAS:
        t = tabela(nome)
        p.append(create_table(t))
        p.append("\n" + indices(t) + "\n")
    p.append("-- registro de criacao para o historico das reunioes e alertas ja existentes\n")
    p.append("INSERT INTO TRANSCRICAO_STATUS_HIST (TRANSCRICAO_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)\n"
             "SELECT ID, NULL, NVL(STATUS, 'PENDENTE'), NVL(DATA_CRIACAO, SYSTIMESTAMP), 'MIGRACAO_003', 'Registro anterior a migracao 003'\n  FROM TRANSCRICOES;\n")
    p.append("INSERT INTO ALERTA_STATUS_HIST (ALERTA_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)\n"
             "SELECT ID, NULL, NVL(STATUS, 'NOVO'), NVL(DATA_CRIACAO, SYSTIMESTAMP), 'MIGRACAO_003', 'Registro anterior a migracao 003'\n  FROM ALERTAS;\nCOMMIT;\n\n")
    for tg in TRIGGERS:
        p.append("-- %s\n%s\n\n" % (tg["comentario"], tg["corpo"]))
    p.append("-- comentarios de todas as tabelas e colunas\n")
    for t in TABELAS:
        p.append(comentarios(t) + "\n")
    return "".join(p)


if __name__ == "__main__":
    print(gerar())
