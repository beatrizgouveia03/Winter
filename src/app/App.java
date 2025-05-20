package app;

import winter.core.ComponentRegistry;
import winter.services.RandomService;

public class App {
    public static void main(String[] args) {
        ComponentRegistry.registerComponent(new RandomService());
    }
    
}