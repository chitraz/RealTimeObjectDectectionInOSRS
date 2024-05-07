import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;

import java.awt.image.BufferedImage;

@ScriptManifest(name = "Data Collection", description = "Collect game screenshots with object class and bounding box annotations", author = "Chitraz",
        version = 1.0, category = Category.MISC, image = "")
public class DataCollect extends AbstractScript {

    @Override
    public int onLoop() {

        Logger.log("Hello world!");


        return 1000;
    }

}
