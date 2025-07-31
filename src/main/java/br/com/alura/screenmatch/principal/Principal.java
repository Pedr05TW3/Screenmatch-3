package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;
import com.fasterxml.jackson.databind.SequenceWriter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=5ec34320&t";
    private List<DadosSerie> dadosSeries = new ArrayList<>();

    // URL: https://www.omdbapi.com/?t=family+guy/&apikey=5ec34320&t


    private SerieRepository repositorio;

    private List<Serie> series = new ArrayList<>();

    private Optional<Serie> serieBusca;

    public Principal(SerieRepository repositorio) {
        this.repositorio = repositorio;
    }

    public void exibeMenu() {
        var opcao = -1;
        while (opcao != 0) {
            var menu = """
                    1 - Buscar séries
                    2 - Buscar episódios
                    3 - Listar séries buscadas
                    4 - Buscar série por título
                    5 - Buscar séries por ator
                    6 - TOP 5 séries
                    7 - Buscar séries por categoria
                    8 - Filtrar séries
                    9 - Buscar episódio por nome
                    10 - Buscar top 5 episódios
                    11 - Buscar episódio a partir de uma data 
                    
                    0 - Sair                                 
                    """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriePorAtor();
                    break;
                case 6:
                    buscarTop5Series();
                    break;
                case 7:
                    buscarSeriePorCategoria();
                    break;
                case 8:
                    buscarSeriePorFiltro();
                    break;
                case 9:
                    buscarEpisodioPortrecho();
                    break;
                case 10:
                    topEpisodiosPorSerie();
                    break;
                case 11:
                    buscarEpisodioAPartirDeUmaData();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);
        repositorio.save(serie);
        //dadosSeries.add(dados);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        listarSeriesBuscadas();
        System.out.println("Digite o nome de uma série: ");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> serie = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serie.isPresent()) {
            var serieEncontrada = serie.get();
            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream().flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());
            serieEncontrada.setEpisodos(episodios);
            repositorio.save(serieEncontrada);

        }else {
            System.out.println("Série não encontrada, tente novamente!");
        }
    }
    private void listarSeriesBuscadas() {
        series = repositorio.findAll();
        series.stream().sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }
    private void buscarSeriePorTitulo() {
        System.out.println("Digite o nome de uma série: ");
        var nomeSerie = leitura.nextLine();
        serieBusca = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serieBusca.isPresent()){
            System.out.println("Dados da série: " + serieBusca.get());
        }else {
            System.out.println("A série não foi encontrada, tente novamente!");
        }
    }
    private void buscarSeriePorAtor() {
        System.out.println("Digite o nome do ator: ");
        var nomeAtor = leitura.nextLine();
        System.out.println("Consultar avaliações a partir de:");
        var avaliacao = leitura.nextDouble();
        List<Serie> serieEncontrada = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);
        System.out.println("Séries encontradas: ");
        serieEncontrada.forEach(System.out::println);
    }
    private void buscarTop5Series(){
        List<Serie> serieTop = repositorio.findTop5ByOrderByAvaliacaoDesc();
        System.out.println("Séries encontradas: ");
        serieTop.forEach(System.out::println);
    }
    private void buscarSeriePorCategoria() {
        System.out.println("Digite a categoria para busca: ");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeGenero);
        List<Serie> seriesPorCategoria = repositorio.findByGenero(categoria);
        if (!repositorio.findByGenero(categoria).isEmpty()){
            System.out.println("Séries encontradas: ");
            seriesPorCategoria.forEach(System.out::println);
        }else{
            System.out.println("Nenhuma série encontrada!");
        }
    }
    private void buscarSeriePorFiltro() {
        System.out.println("Digite a quantidade máxima de temporadas: ");
        var totalTemporada = leitura.nextInt();
        System.out.println("Digite a avaliação mínima: ");
        var avaliacao = leitura.nextDouble();
        List<Serie> seriesPorFiltro = repositorio.seriesPorTemporadaEAvaliacao(totalTemporada, avaliacao);
        seriesPorFiltro.forEach(System.out::println);
    }
    private void buscarEpisodioPortrecho() {
        System.out.println("Digite o nome do episódio: ");
        var trechoEpisodio = leitura.nextLine();
        List<Episodio> episodiosEncontrados = repositorio.episodiosPorTrecho(trechoEpisodio);
        episodiosEncontrados.forEach(e ->
                System.out.printf("Série: %s Temporada %s - Episódio %s - %s\n",
                        e.getSerie().getTitulo(), e.getTemporada(),
                        e.getNumeroEpisodio(), e.getTitulo()));
    }
    private void topEpisodiosPorSerie() {
        buscarSeriePorTitulo();
        if (serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            List<Episodio> topEpisodios = repositorio.topEpisodiosPorSerie(serie);
            topEpisodios.forEach(e ->
                    System.out.printf("Série: %s Temporada %s - Episódio %s - %s - Nota: %s\n",
                            e.getSerie().getTitulo(), e.getTemporada(),
                            e.getNumeroEpisodio(), e.getTitulo(), e.getAvaliacao()));
        }
    }
    private void buscarEpisodioAPartirDeUmaData() {
buscarSeriePorTitulo();
if (serieBusca.isPresent()){
    Serie serie = serieBusca.get();
    System.out.println("Digite o ano de lançamento: ");
var anoLancamento = leitura.nextLine();
leitura.nextLine();
List<Episodio> episodiosAno = repositorio.episodiosPorSerieEAno(serie, anoLancamento);
episodiosAno.forEach(System.out::println);
}
    }
}