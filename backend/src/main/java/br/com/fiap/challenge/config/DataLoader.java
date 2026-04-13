package br.com.fiap.challenge.config;

import br.com.fiap.challenge.dto.TranscricaoRequest;
import br.com.fiap.challenge.repository.TranscricaoRepository;
import br.com.fiap.challenge.service.TranscricaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    @Bean
    @Profile("demo")
    public CommandLineRunner loadDemoData(TranscricaoRepository repo, TranscricaoService service) {
        return args -> {
            if (repo.count() > 0) {
                log.info("Dados de exemplo ja existem, pulando carga.");
                return;
            }

            log.info("Carregando dados de exemplo...");

            service.criarEAnalisar(TranscricaoRequest.builder()
                    .textoOriginal("Bom dia, João. Então, o nosso Protheus está atendendo o backoffice, mas o time de RH "
                            + "está sofrendo muito com a folha manual. O pessoal viu uma demo da Senior e gostou, mas eu "
                            + "prefiro consolidar tudo na TOTVS se o RM for realmente integrado. Ah, e por favor, não "
                            + "mencione esse valor de R$ 50 mil que conversamos para o meu CFO ainda, ele está focado no "
                            + "ROI do semestre.")
                    .cliente("Empresa ABC Ltda")
                    .vendedor("João Silva")
                    .participantes("João Silva (TOTVS), Carlos Mendes (ABC)")
                    .duracaoMinutos(45)
                    .build());

            service.criarEAnalisar(TranscricaoRequest.builder()
                    .textoOriginal("Olá, Maria. Estamos muito satisfeitos com o Protheus, ele atendeu todas as nossas "
                            + "expectativas no financeiro e contábil. Agora precisamos expandir para o módulo de compras "
                            + "e estoque. Temos um orçamento de R$ 80 mil aprovado para o próximo trimestre e queremos "
                            + "começar a implantação o quanto antes. A diretoria está comprometida com a digitalização "
                            + "completa dos processos.")
                    .cliente("Tech Solutions SA")
                    .vendedor("Maria Oliveira")
                    .participantes("Maria Oliveira (TOTVS), Ana Costa (Tech Solutions)")
                    .duracaoMinutos(30)
                    .build());

            service.criarEAnalisar(TranscricaoRequest.builder()
                    .textoOriginal("Sinceramente, estou frustrado com o suporte. Já abri três chamados sobre o erro no "
                            + "módulo fiscal e ninguém resolve. Meu contador está reclamando todos os dias. Se isso não "
                            + "for resolvido até o final do mês, vou ter que avaliar outras opções. Ouvi falar bem do "
                            + "Sankhya e do SAP Business One. Não quero trocar, mas preciso de uma solução que funcione.")
                    .cliente("Distribuidora Delta")
                    .vendedor("Pedro Santos")
                    .participantes("Pedro Santos (TOTVS), Roberto Lima (Delta)")
                    .duracaoMinutos(20)
                    .build());

            log.info("Dados de exemplo carregados com sucesso!");
        };
    }
}
