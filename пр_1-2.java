public class Main {

    @FunctionalInterface
    interface Transformer<T, R> {
        R apply(T value);

        default <V> Transformer<T, V> andThen(Transformer<? super R, ? extends V> after) {
            Objects.requireNonNull(after, "after");
            return (T t) -> after.apply(this.apply(t));
        }

        static <T> Transformer<T, T> identity() {
            return t -> t;
        }
    }


    static class Utils {
        static String trimToUpper(String s) {
            return s.trim().toUpperCase(Locale.ROOT);
        }
    }

    static class Pipeline {
        static int process(List<String> input, Consumer<String> logger) {
            Predicate<String> notBlank = s -> s != null && !s.isBlank();
            Function<String, String> normalize = s -> s.trim().toLowerCase(Locale.ROOT);
            Function<String, Integer> toLen = String::length;

            List<Integer> lengths = input.stream()
                    .filter(notBlank)
                    .map(normalize)
                    .peek(logger)
                    .map(toLen)
                    .toList(); 

            return lengths.stream().mapToInt(Integer::intValue).sum();
        }
    }

    static class Composition {
        static void demo() {
            Function<Integer, Integer> add2 = x -> x + 2;
            Function<Integer, Integer> times3 = x -> x * 3;

            int a = add2.andThen(times3).apply(5); // (5+2)*3 = 21
            int b = add2.compose(times3).apply(5); // (5*3)+2 = 17
            System.out.println("[compose demo #1] " + a + " vs " + b);

            Function<String, String> trim = String::trim;
            Function<String, String> wrap = s -> "[" + s + "]";

            String s1 = trim.andThen(wrap).apply("  hi  "); // "[hi]"
            String s2 = trim.compose(wrap).apply("  hi  "); // "[  hi  ]"
            System.out.println("[compose demo #2] " + s1 + " vs " + s2);
        }
    }

    static class Perf {
        static int loop(List<String> input) {
            int sum = 0;
            for (String s : input) {
                if (s != null && !s.isBlank()) {
                    String n = s.trim().toLowerCase(Locale.ROOT);
                    sum += n.length();
                }
            }
            return sum;
        }

        static int stream(List<String> input) {
            return input.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> s.trim().toLowerCase(Locale.ROOT))
                    .mapToInt(String::length)
                    .sum();
        }

        static void run() {
            List<String> data = new ArrayList<>(10_000);
            for (int i = 0; i < 10_000; i++) {
                data.add(i % 10 == 0 ? "   " : "  Abc" + i + "  ");
            }

            
            for (int i = 0; i < 5; i++) { loop(data); stream(data); }

            long t1 = System.nanoTime();
            int a = loop(data);
            long t2 = System.nanoTime();
            int b = stream(data);
            long t3 = System.nanoTime();

            System.out.println("[perf] loop   sum=" + a + " time(ns)=" + (t2 - t1));
            System.out.println("[perf] stream sum=" + b + " time(ns)=" + (t3 - t2));
        }
    }

    
    public static void main(String[] args) throws Exception {
        
        Transformer<String, Integer> len = s -> s.length();
        Transformer<String, String> norm = Utils::trimToUpper;     
        String prefix = "ID:";
        Transformer<String, String> addPrefix = prefix::concat;    

        System.out.println("[lambda] len=" + len.apply("Hello"));
        System.out.println("[method ref] norm=" + norm.apply("  hello "));
        System.out.println("[instance ref] prefix=" + addPrefix.apply("123"));

        
        AtomicInteger logged = new AtomicInteger(0);
        int sum = Pipeline.process(
                List.of("  a  ", "", " bb", null, "CCC "),
                s -> logged.incrementAndGet()
        );
        System.out.println("[pipeline] sum=" + sum + " logged=" + logged.get());

       
        Composition.demo();

        
        Perf.run();

       
        Stream.iterate(0, n -> n + 1).limit(5).forEach(n -> System.out.print(n + " "));
        System.out.println();
    }
}
