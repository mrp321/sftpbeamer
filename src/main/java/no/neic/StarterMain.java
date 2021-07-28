package no.neic;

import io.vertx.core.Launcher;
import no.neic.tryggve.App;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StarterMain {
    public static void main(String[] args) throws IOException {
        List<String> argList = new ArrayList<>(Arrays.asList(args));
        argList.add("run");
        argList.add(App.class.getName());
        Launcher.main(argList.toArray(new String[]{}));
    }
}
