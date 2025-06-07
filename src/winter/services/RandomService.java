package winter.services;

import winter.annotations.RemoteMethod;
import winter.annotations.RemoteComponent;

@RemoteComponent(name = "Random")
public class RandomService {
    
    /**
     * Generates a random integer between min and max (inclusive).
     *
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     * @return a random integer between min and max
     */
    @RemoteMethod
    public int randomInt(Integer min, Integer max) {
        return (int) (Math.random() * (max - min + 1)) + min;
    }

    /**
     *Generates a random password of the specified length.
     *
     * @param length the length of the password
     * @return a random password    
     */
    @RemoteMethod
    public String randomPassword(Integer length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }

        return password.toString();
    }

    /**
     * Generates a random username
     *
     * @return a random username
     */
    @RemoteMethod
    public String randomUsername() {
        String[] names = {"Alice", "Bob", "Charlie", "David", "Eve"};
        int index = (int) (Math.random() * names.length);
        return names[index];
    }
}
