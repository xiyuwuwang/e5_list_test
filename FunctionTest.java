    @Test
    public void test8848() {
//        Function<String, String> a= b->"你好"+b;
        System.out.println(bt("66").apply("开心"));
    }

    public Function<String, String> bt(String t){

        return b->t+b;
    }
