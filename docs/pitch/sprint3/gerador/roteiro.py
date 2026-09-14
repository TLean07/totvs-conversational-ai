# -*- coding: utf-8 -*-
"""Gera o PDF do roteiro de fala do Pitch Sprint 3 (um bloco por slide)."""
import html
import os
import shutil
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(AQUI, "..", "..", "..", "database-design", "sprint3", "gerador"))
from documento import gerar_pdf  # noqa: E402

SAIDA = os.path.abspath(os.path.join(AQUI, ".."))
ENTREGA = r"C:\Users\Leandro\Downloads\Sprint3"

SLIDES = [
    ("Capa", "Bom dia a todos. Somos a Company 404 e vamos apresentar o TOTVS Conversational Intelligence, uma plataforma que transforma as reuniões comerciais da TOTVS em inteligência para vender mais e perder menos clientes."),
    ("A TOTVS", "A TOTVS é a maior empresa de software de gestão do Brasil, com mais de 70 mil clientes. Para atender essa base, os times de Vendas e Customer Success fazem milhares de reuniões por dia. Cada conversa tem sinais de compra, de insatisfação e de concorrência. O problema é que tudo isso fica preso em transcrições que ninguém tem tempo de ler."),
    ("O Problema", "Isso gera três problemas. Primeiro, o risco é identificado tarde: a empresa só percebe que vai perder o cliente quando ele já pediu o cancelamento. Segundo, oportunidades de venda se perdem, porque o interesse e o orçamento citados na reunião nunca viram proposta. Terceiro, a liderança fica sem visibilidade e recebe essas informações com dias ou semanas de atraso. O resultado é receita que escapa."),
    ("Proposta de Valor", "Nossa proposta é transformar cada reunião em inteligência de negócio. Isso traz agilidade, porque o alerta chega em minutos e não em dias. Permite atuar antes do cancelamento, mapeando insatisfação e concorrentes. E aumenta o valor do cliente, porque cada gatilho de compra vira uma oportunidade clara de upsell ou cross-sell."),
    ("TOTVS Conversational Intelligence", "A solução recebe a reunião por texto, pelos arquivos da própria TOTVS ou por áudio. Ela extrai tudo automaticamente, sem análise manual e sem depender de API paga de IA. Detecta risco de churn, analisa o sentimento, identifica quem é o interlocutor e qual o orçamento, aponta concorrentes, gera alertas e aprende com o feedback de quem usa."),
    ("Como Funciona", "O funcionamento é simples. A reunião entra na plataforma e a LeanIA, nosso motor de IA próprio, analisa a conversa. O resultado fica guardado no Oracle, e o time age pelo dashboard na web ou no celular. Nossos diferenciais são três: não tem custo de API, conhece os produtos e concorrentes da TOTVS e já está funcionando em produção."),
    ("Na Prática", "Vejam um exemplo real de um cliente da área da saúde. O cliente disse que estava com problemas e pensando em trocar de sistema. A LeanIA identificou uma oportunidade de venda do TOTVS Fluig, com 90% de confiança, e um risco de insatisfação. Também identificou o cargo do interlocutor e um orçamento de 10 mil reais, e já sugeriu a próxima ação: enviar a proposta e agendar uma demonstração."),
    ("Resultados Reais", "E isso não é simulação. Analisamos 623 reuniões reais da base da TOTVS, todas processadas automaticamente. Encontramos 960 oportunidades de venda, 579 riscos, sendo 150 de churn, e geramos 244 alertas para a liderança. Um dado chama atenção: quase metade das conversas é mista, com elogio e reclamação juntos. É exatamente aí que o risco fica escondido."),
    ("Visibilidade Web", "Esta é a plataforma web, que já está no ar pelo link do slide. O dashboard mostra as métricas principais logo na primeira tela, e as páginas de oportunidades e riscos trazem a evidência da conversa e a ação recomendada. Tudo foi pensado para o time de vendas usar sem treinamento."),
    ("Visibilidade Mobile", "Também temos o aplicativo Android, com os mesmos dados da web. O gestor acompanha o painel, as oportunidades, os riscos e os alertas entre uma reunião e outra, direto do celular."),
    ("Próximos Passos", "Para a próxima sprint, vamos treinar um modelo de machine learning com as reuniões reais para a LeanIA. Também vamos adicionar filtros no dashboard, garantir a escala com testes de carga, deixar os alertas em linguagem de negócio e entregar o vídeo do MVP com a visão de custo do produto."),
    ("Nossa Equipe", "Essa é a nossa equipe: Bruno, Kauai, Lucas, Leandro e Murilo, a Company 404."),
    ("Obrigado", "Muito obrigado pela atenção. A plataforma está disponível no link abaixo para vocês testarem. Ficamos à disposição para perguntas."),
]

CSS = """
@page { size: A4; margin: 25mm 20mm 25mm 25mm; }
body { font-family: 'Times New Roman', Times, serif; font-size: 12pt; color: #000; line-height: 1.45; }
.cab { text-align: center; margin-bottom: 22px; }
.cab .t1 { font-size: 16pt; font-weight: bold; }
.cab .t2 { font-size: 12pt; margin-top: 4px; }
.slide { margin: 0 0 16px; page-break-inside: avoid; }
.slide h2 { font-size: 12.5pt; font-weight: bold; margin: 0 0 4px; }
.slide p { margin: 0; text-align: justify; }
"""


def montar_html():
    blocos = []
    for i, (titulo, fala) in enumerate(SLIDES, 1):
        blocos.append('<div class="slide"><h2>Slide %d - %s</h2><p>"%s"</p></div>'
                      % (i, html.escape(titulo), html.escape(fala)))
    return ('<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><title>Roteiro do Pitch</title>'
            '<style>%s</style></head><body><div class="cab"><div class="t1">Roteiro de Apresentação do Pitch</div>'
            '<div class="t2">TOTVS Conversational Intelligence | Company 404 | Sprint 3</div></div>%s</body></html>'
            % (CSS, "".join(blocos)))


def main():
    html_path = os.path.join(SAIDA, "roteiro_pitch.html")
    pdf_path = os.path.join(SAIDA, "Company404_Roteiro_Pitch_Sprint3.pdf")
    with open(html_path, "w", encoding="utf-8") as f:
        f.write(montar_html())
    gerar_pdf(html_path, pdf_path)
    if os.path.isdir(ENTREGA):
        shutil.copy2(pdf_path, ENTREGA)
    print(pdf_path)


if __name__ == "__main__":
    main()
