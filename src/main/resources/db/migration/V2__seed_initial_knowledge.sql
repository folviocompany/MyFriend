INSERT INTO knowledge_article (title, category, content) VALUES
(
    'LED LOS aceso ou piscando na ONU',
    'FIBRA_ONU',
    '[Rascunho inicial — revisar com analista] O LED LOS indica perda ou ausência do sinal óptico recebido pela ONU. Confirme o estado dos LEDs, verifique se o cordão óptico está dobrado, prensado, desconectado ou com conectores visivelmente danificados. Evite tocar nas extremidades dos conectores. Compare com alarmes da rede somente quando houver acesso autorizado. Não recomende reset de fábrica: essa ação pode apagar a configuração e prolongar a indisponibilidade. Se a inspeção local não explicar o alarme, encaminhe para medição óptica e análise da rede externa.'
),
(
    'Investigação de perda de pacotes',
    'LATENCIA_PERDA_PACOTES',
    '[Rascunho inicial — revisar com analista] Diferencie perda no acesso local de perda além da rede do provedor. Colete testes por cabo sempre que possível, compare múltiplos destinos e observe em qual trecho a perda começa e se continua nos saltos seguintes. Um roteador intermediário pode limitar respostas de diagnóstico sem descartar o tráfego encaminhado; por isso, não conclua falha apenas por um salto isolado. Correlacione horário, recorrência e quantidade de clientes afetados antes de escalar.'
),
(
    'Latência elevada em jogos online',
    'LATENCIA_PERDA_PACOTES',
    '[Rascunho inicial — revisar com analista] Primeiro separe latência local, latência até a rede do provedor e latência do caminho externo até o servidor do jogo. Prefira teste cabeado, interrompa tráfego intenso autorizado e compare ping e traceroute para destinos distintos. Verifique perda, variação de latência, saturação do enlace e uso de Wi-Fi. A rota até um servidor pode ser diferente da rota até outro; evite atribuir a causa ao provedor sem evidências do trecho afetado.'
),
(
    'MTU e sintomas de fragmentação',
    'ROTEADORES_CONFIGURACOES',
    '[Rascunho inicial — revisar com analista] Suspeite de MTU quando alguns sites ou aplicações funcionam e outros travam, especialmente em túneis ou conexões que adicionam encapsulamento. Antes de alterar a configuração, confirme o tipo de acesso e execute testes não invasivos de tamanho de pacote quando autorizados. Alterar a MTU incorretamente pode reduzir desempenho ou interromper sessões; registre o valor anterior e planeje reversão antes de qualquer mudança.'
),
(
    'Diferença entre CGNAT e endereço IP público',
    'CGNAT_NAT',
    '[Rascunho inicial — revisar com analista] CGNAT compartilha um endereço público entre vários assinantes e pode impedir conexões iniciadas da internet para dentro da rede do cliente. Um endereço público permite exposição direta, mas ainda depende de firewall e configuração adequada. Compare o endereço recebido pelo equipamento de borda com o endereço observado externamente sem divulgar dados do cliente. Aplicações que exigem encaminhamento de portas podem precisar de alternativa contratual ou técnica.'
),
(
    'Diagnóstico de DNS e conectividade IPv6',
    'DNS_IPV6',
    '[Rascunho inicial — revisar com analista] Para DNS, diferencie falha de resolução de falha de conectividade testando nomes e destinos conhecidos conforme a política operacional. Para IPv6, confirme se o cliente recebeu prefixo, rota padrão e servidores DNS, e compare o comportamento com IPv4. Não desative IPv6 como primeira medida: isso pode apenas ocultar a causa e afetar aplicações. Registre quais protocolos e destinos apresentam falha.'
),
(
    'Como interpretar um traceroute',
    'DIAGNOSTICO_CONECTIVIDADE',
    '[Rascunho inicial — revisar com analista] O traceroute mostra respostas dos roteadores ao longo de um caminho possível, não uma medição completa de cada enlace. Ausência ou alta latência em um salto isolado pode representar limitação de resposta de diagnóstico. Considere problema quando a anomalia começa em um ponto e persiste nos saltos posteriores, de forma reproduzível. Rotas de ida e volta podem ser diferentes; correlacione o resultado com ping, perda e horário.'
),
(
    'Interferência e canais em redes Wi-Fi',
    'WIFI',
    '[Rascunho inicial — revisar com analista] Compare o serviço por cabo e por Wi-Fi antes de atribuir a falha ao acesso. Verifique distância, obstáculos, interferência, ocupação de canais e quantidade de dispositivos ativos. A troca de canal ou banda pode desconectar dispositivos temporariamente e deve ser combinada com o cliente. Evite prometer velocidade ou cobertura sem considerar o ambiente e as capacidades dos dispositivos.'
);
