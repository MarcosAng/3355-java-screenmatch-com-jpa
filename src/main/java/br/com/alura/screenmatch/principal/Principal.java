package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.Categoria;
import br.com.alura.screenmatch.model.DadosSerie;
import br.com.alura.screenmatch.model.DadosTemporada;
import br.com.alura.screenmatch.model.Episodio;
import br.com.alura.screenmatch.model.Serie;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

import java.util.Comparator;


public class Principal {

    private SerieRepository repositorio;

    private List<Serie> series = new ArrayList<>();

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";

    private List<DadosSerie> dadosSeries = new ArrayList<>();

    
    public Principal(SerieRepository repositorio ){
        this.repositorio = repositorio;
    }

    public void exibeMenu() {
        var opcao =-1;
        while (opcao !=0) {
           var menu = """
                1 - Buscar séries
                2 - Buscar episódios
                3 - Listar séries buscadas
                4 - Buscar série por título
                5 - Buscar séries por ator
                6 - Top 5 Séries
                7 - Buscar séries por Gênero
                8 - Buscar séries por temporada e avaliação
                9 - Buscar Episódios por trecho
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
                buscarSeriesPorAtor();
                break;
            case 6:
                buscarTop5Series();
                break;
            case 7:
                buscarSeriesPorCategoria();
                break;
            case 8:
                buscarSeriesPorNumeroTemporadaAvaliacaoMinima();
                break;
            case 9:
                buscarEpisodiosPorTrecho();
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
        System.out.println("Escolha uma série pelo nome: ");
        var nomeSerie = leitura.nextLine();
        Optional<Serie> serie = repositorio.findByTituloContainingIgnoreCase(nomeSerie);
        //series.stream().filter(s-> s.getTitulo().toLowerCase().contains(nomeSerie.toLowerCase())).findFirst();
        if(serie.isPresent()){
            var serieEncontrada = serie.get();

            List<DadosTemporada> temporadas = new ArrayList<>();

        for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
            var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
            DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
            temporadas.add(dadosTemporada);
        }
        temporadas.forEach(System.out::println);
        List<Episodio> episodios = temporadas.stream()
                                              .flatMap(d -> d.episodios().stream()
                                               .map(e-> new Episodio(d.numero(), e))).collect(Collectors.toList());
        serieEncontrada.setEpisodios(episodios);
        repositorio.save(serieEncontrada);

        }else{
            System.out.println("Série não encontrada!");
        }
        
    }

    private void listarSeriesBuscadas(){
        series = repositorio.findAll();
        series.stream().sorted(Comparator.comparing(Serie::getGenero))
        .forEach(System.out::println);        
    }

    private void buscarSeriePorTitulo(){
        System.out.println("Informe o título ou parte do título da série:");
        var nomeSerie = leitura.nextLine();
        Optional<Serie> serieEncontrada = repositorio.findByTituloContainingIgnoreCase(nomeSerie);
        if(serieEncontrada.isPresent()){
            System.out.println("Dados da série: " + serieEncontrada.get());
        }else{
            System.out.println("Série não encontrada!");
        }
    }

    private void buscarSeriesPorAtor(){
        System.out.println("Informe o nome do(a) ator/atriz:");
        var nomeAtor = leitura.nextLine();
        System.out.println("Avaliações a partir de que valor? ");
        var avaliacao = leitura.nextDouble();
        List<Serie> seriesEncontradas = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);
        if(seriesEncontradas.isEmpty()){
            System.out.println("Nenhuma série encontrada para o(a) ator/atriz " + nomeAtor);
        }else{
            System.out.println("As séries encontradas em que o(a) ator/atriz " + nomeAtor + " trabalhou: " );
            seriesEncontradas.forEach(s -> System.out.println(s.getTitulo() + 
                                    " avaliação "+ s.getAvaliacao()));
        }        
    }

    private void buscarTop5Series(){
        List<Serie> seriesTop = repositorio.findTop5ByOrderByAvaliacaoDesc();
        if(seriesTop.isEmpty()){
            System.out.println("Nenhuma série encontrada!");
        }else{
            System.out.println("As séries top 5 são: " );
            seriesTop.forEach(s -> System.out.println(s.getTitulo() + 
                                    " avaliação "+ s.getAvaliacao()));
        }        
    }

    private void buscarSeriesPorCategoria(){
        System.out.println("Deseja pesquisar séries de qual categoria/gênero ?");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeGenero);
        //List<Serie> seriesPorCategoria = repositorio.findByGenero(categoria);
        List<Serie> seriesPorCategoria = repositorio.findByGenero(categoria);        
        if(seriesPorCategoria.isEmpty() || seriesPorCategoria == null){
            System.out.println("Nenhuma série encontrada para a categoria/gênero: " + categoria.name());
            System.out.println(" ");
        }else{
            System.out.println("Séries da Categoria/Gênero "+ categoria.name());
            seriesPorCategoria.forEach(s -> System.out.println(s.getTitulo() + " Avaliação "+ s.getAvaliacao()));
            System.out.println(" ");
        }        
    }

    private void buscarSeriesPorNumeroTemporadaAvaliacaoMinima(){
        System.out.println("Buscar séries até quantas temporadas?");
        var totalTemporadas = leitura.nextInt();
        System.out.println("Avaliações a partir de que valor ?");
        var avaliacao = leitura.nextDouble();
        List<Serie> series = repositorio.findbyTotalTemporadasEAvaliacao(totalTemporadas, avaliacao);
        if(series.isEmpty() || series == null){
            System.out.println("Nenhuma série encontrada.");
            System.out.println(" ");
        }else{
            System.out.println("*** Séries encontradas ****");
            series.forEach(s -> System.out.println(s.getTitulo() +  " Avaliação: "+s.getAvaliacao()));
            System.out.println(" ");
        }
    }

    private void buscarEpisodiosPorTrecho(){
        System.out.println("Informe o trecho do episódio:");
        var trechoEpisodio = leitura.nextLine();
        List<Episodio> episodios = repositorio.buscarEpisodiosPorTrecho(trechoEpisodio);
        if(episodios.isEmpty() || episodios == null){
            System.out.println("Nenhum episódio encontrado.");
            System.out.println(" ");
        }else{
            System.out.println("*** Episódios encontrados ****");
            episodios.forEach(e -> System.out.println("Série: "+ e.getSerie().getTitulo()+ " Episódio: " + e.getTitulo() +  " Avaliação: "+e.getAvaliacao()));
            System.out.println(" ");
        }
    }
}