    @Test
    public void test8848() {
//        Function<String, String> a= b->"你好"+b;
        System.out.println(bt("66").apply("开心"));
    }
/**
    * 返回一个组合函数, 首先执行before function的apply方法, 将它的返回作为输入参数再应用当前的function
    这炸糕
    */
    default <V> Function<V, R> compose(Function<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        return (V v) -> apply(before.apply(v));
    }
    public Function<String, String> bt(String t){

        return b->t+b;
    }
