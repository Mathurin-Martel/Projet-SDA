import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

public class AppelPythonTest{
    ArrayList<String[]> data=new ArrayList<>();
    public static void main(String[] args) throws Exception {
        AppelPythonTest APT=new AppelPythonTest();
        ProcessBuilder builder = new ProcessBuilder("python", "conversion_osmnx.py");
        Process process = builder.start();

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream())
        );

        BufferedReader errorReader = new BufferedReader(
            new InputStreamReader(process.getErrorStream())
        );

        String ligne;

        // lire en continu
        while ((ligne = reader.readLine()) != null) {
            APT.data.add(ligne.split("\\s+"));
        }

        while ((ligne = errorReader.readLine()) != null) {
            System.out.println("ERROR");
        }

        process.waitFor();

        for (String[] line : APT.data) {
            System.out.println(java.util.Arrays.toString(line) + ", ");
        }
        System.out.println(APT.data.get(0)[1]);
    }
}
