"""
Testes da LeanIA contra o exemplo oficial do briefing (Challenge FIAP x TOTVS, pagina 15).
Rodar: python -m pytest tests/  (ou python tests/test_analyzer.py)
"""

import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from analyzer import TranscricaoAnalyzer  # noqa: E402

TEXTO_BRIEFING = (
    "Bom dia, João. Então, o nosso Protheus está atendendo o backoffice, mas o time de RH "
    "está sofrendo muito com a folha manual. O pessoal viu uma demo da Senior e gostou, mas eu "
    "prefiro consolidar tudo na TOTVS se o RM for realmente integrado. Ah, e por favor, não "
    "mencione esse valor de R$ 50 mil que conversamos para o meu CFO ainda, ele está focado no "
    "ROI do semestre."
)

TEXTO_DIALOGO = (
    "[LOCUTOR 1]: Boa tarde! Obrigado por vir. Me conta sobre seu negócio.\n"
    "[LOCUTOR 2]: A gente atua em varejo há 10 anos. Temos 15 lojas em São Paulo.\n"
    "[LOCUTOR 1]: E qual é o principal desafio?\n"
    "[LOCUTOR 2]: Controle de estoque. A gente perde muito tempo com isso, é tudo manual e a gente perde venda.\n"
    "[LOCUTOR 1]: Nossa solução conecta todas as lojas em uma plataforma integrada.\n"
    "[LOCUTOR 2]: Quanto custa? E quanto tempo leva?\n"
    "[LOCUTOR 1]: Uns 15 a 20 mil por mês e uns 30 dias no máximo. Vou enviar a proposta formal.\n"
    "[LOCUTOR 2]: Pode mandar. Preciso conversar com meu sócio, amanhã a gente responde.\n"
)


def _analyzer():
    return TranscricaoAnalyzer(enable_transformers=False)


def test_exemplo_do_briefing_entrega_o_resultado_esperado():
    r = _analyzer().analisar(TEXTO_BRIEFING)
    produtos = {p["nomeProduto"]: p["tipo"] for p in r["produtos"]}

    assert produtos["TOTVS Protheus"] == "ATUAL"
    assert produtos["TOTVS RM"] == "OPORTUNIDADE"
    assert produtos["Senior"] == "CONCORRENTE"

    conc = [x for x in r["riscos"] if x["tipo"] == "CONCORRENCIA"]
    assert conc and conc[0]["severidade"] == "ALTO" and conc[0]["concorrenteMencionado"] == "Senior"
    assert "RH" in conc[0]["descricao"]

    assert r["oportunidades"], "esperava oportunidade de upsell"
    ups = r["oportunidades"][0]
    assert ups["tipo"] == "UPSELL"
    assert "Folha de Pagamento" in ups["descricao"]
    assert ups["produtoRelacionado"] == "TOTVS RM"
    assert ups["score"] >= 0.8
    assert ups["valorEstimado"] == "R$ 50.000,00"

    assert r["personaIdentificada"].startswith("Gestor Operacional")
    assert r["budgetIdentificado"] == "R$ 50.000,00"
    assert r["sentimentoGeral"] == "MISTO"
    assert "Backoffice" in r["sentimentoDetalhe"] and "RH" in r["sentimentoDetalhe"]
    assert any("CFO" in o for o in r["insightsAdicionais"]["observacoes"])
    assert any("discricao" in o for o in r["insightsAdicionais"]["observacoes"])
    assert r["scoreConfiancaVendedor"] >= 0.6
    assert r["alertasConsolidados"] and r["alertasConsolidados"][0]["categoria"] == "Concorrencia"


def test_dialogo_com_locutores_identifica_papeis_e_jornada():
    r = _analyzer().analisar(TEXTO_DIALOGO)
    ind = r["indicadoresQualidade"]
    assert ind["locutores"] == 2
    assert ind["participacaoVendedorPct"] + ind["participacaoClientePct"] == 100
    assert ind["perguntasVendedor"] >= 1 and ind["perguntasCliente"] >= 2
    assert r["insightsAdicionais"]["duvidasJornada"], "cliente perguntou preco e prazo"
    assert r["insightsAdicionais"]["proximosPassos"], "vendedor vai enviar proposta"
    assert r["budgetIdentificado"] == "R$ 15.000,00 a R$ 20.000,00 por mes"
    ops = r["oportunidades"]
    assert ops and ops[0]["tipo"] == "NOVO_PRODUTO" and "Estoque" in ops[0]["descricao"]
    assert any(x["tipo"] == "INSATISFACAO" for x in r["riscos"]), "dor com controle de estoque"
    assert not any(x["tipo"] == "CHURN" for x in r["riscos"])


def test_cancelar_reuniao_nao_e_churn_mas_cancelar_contrato_e():
    a = _analyzer()
    r1 = a.analisar("[LOCUTOR 1]: Vamos precisar cancelar a reuniao de amanha, pode ser?")
    assert not any(x["tipo"] == "CHURN" for x in r1["riscos"])
    r2 = a.analisar("[LOCUTOR 2]: Se continuar assim vamos cancelar o contrato e migrar para outro fornecedor.")
    assert any(x["tipo"] == "CHURN" for x in r2["riscos"])


def test_negacao_nao_conta_como_problema():
    r = _analyzer().analisar("[LOCUTOR 2]: Nao temos nenhum problema com o Protheus, esta funcionando bem.")
    assert r["sentimentoGeral"] == "POSITIVO"
    assert not r["riscos"]


if __name__ == "__main__":
    for nome, fn in list(globals().items()):
        if nome.startswith("test_") and callable(fn):
            fn()
            print("ok ", nome)
    print(json.dumps(_analyzer().analisar(TEXTO_BRIEFING), ensure_ascii=False, indent=2))
