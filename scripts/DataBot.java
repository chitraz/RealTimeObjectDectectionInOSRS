import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage; 
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.Color;
import java.awt.geom.PathIterator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.dreambot.api.Client;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.input.Camera;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.wrappers.interactive.Player;        
import org.dreambot.api.wrappers.interactive.Entity;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.items.GroundItem;
import org.dreambot.api.wrappers.interactive.Model;

@ScriptManifest(name = "Detection Data Collector", description = "Walk in a given path, "
        + "sets random camera angle/zoom and save client screenshots along with their annoations. "
        + " For all game entities in views, save their bounding boxes and segmentation shapes."
        , author = "chitraz", version = 1.0, category = Category.UTILITY, image = "")
public class DataBot extends AbstractScript {
    
    // paths and annotation file names
    public String root_dir = "/home/chitraz/Documents/Projects/RealTimeODinOSRS/dataset/raw/";
    public String npc_file = "annotations/raw_NPCs.txt";
    public String player_file = "annotations/raw_Players.txt";
    public String go_file = "annotations/raw_GameObjects.txt";
    public String item_file = "annotations/raw_GroundItems.txt";
    
    // list of NPCs 
    public List<NPC> npcs; 
    // list of Players
    public List<Player> players;
    // list of Game-Objects
    public List<GameObject> objects;
    // list of Ground items
    public List<GroundItem> items;
    
    // list of bounding boxes for the found entities
    public List<Rectangle> npcs_BBs = new ArrayList<>(); 
    public List<Rectangle> players_BBs = new ArrayList<>();  
    public List<Rectangle> objects_BBs = new ArrayList<>(); 
    public List<Rectangle> items_BBs = new ArrayList<>(); 
    //list of segmentation masks
    public List<Area> npcs_masks = new ArrayList<>(); 
    public List<Area> players_masks = new ArrayList<>(); 
    public List<Area> objects_masks = new ArrayList<>(); 
    public List<Area> items_masks = new ArrayList<>(); 
    // list of coresponding unique ID numbers
    public List<Integer> npcs_UIDs = new ArrayList<>();  
    public List<Integer> players_UIDs = new ArrayList<>();  
    public List<Integer> objects_UIDs = new ArrayList<>();  
    public List<Integer> items_UIDs = new ArrayList<>(); 
    // list of coresponding names
    public List<String> npcs_names = new ArrayList<>(); 
    public List<String> players_names = new ArrayList<>(); 
    public List<String> objects_names = new ArrayList<>(); 
    public List<String> items_names = new ArrayList<>(); 
    
    // flags
    public boolean isSample = false;
    // image name
    public String imageName; 
    
    // 520x340 viewport 
    Rectangle viewport = new Rectangle(0,0,520,340);
  
    @Override
    public void onStart() {
        Logger.log("Initialising bot...");
        this.getEntities();
    }

    @Override
    public int onLoop() {
        int sample_freq = 15; // sample frequency (in number of game tiles walked)
        int num_viewpoints = 10;// number of random viewpoints per location to use when collection samples
        int step_count = 0;

        for (Tile targetTile: this.path){
            // print progress 
            Logger.log("walking to target tile: (" + Integer.toString(targetTile.getX()) + "," 
                    + Integer.toString(targetTile.getY()) + "," + Integer.toString(targetTile.getZ()) + ")");
            
            // walk to target tile in path
            Walking.walk(targetTile); 
            step_count++;            
            
            // get all NPCs, Game Objects, players and ground items currently visible
            this.getEntities();
            
            // save a sample every sample_freq many tiles walked (approx.)
            if(step_count == sample_freq - 1){ 
                Logger.log("----------------Stopping for picture--------------------- ");
                // wait for bot to come to a stop or 10 sec passes
                Sleep.sleepUntil(() -> Players.getLocal().isMoving() == false, 10000);
                for (int i = 0; i < num_viewpoints; i++) {
                    
                    // get a random viewpoint 
                    Logger.log("Setting a random camera angle and zoom...");
                    RandomCameraView();
                    
                    //move cursor away before taking image
                    Mouse.moveOutsideScreen(); 

                    // Create a unique image file name [TileX_TileY_TileZ_Zoom_Pitch_Yaw]
                    String tile_x = Integer.toString(Players.getLocal().getTile().getX());
                    String tile_y = Integer.toString(Players.getLocal().getTile().getY());
                    String tile_z = Integer.toString(Players.getLocal().getTile().getZ());
                    String zoom = Integer.toString(Camera.getZoom());
                    String pitch = Integer.toString(Camera.getPitch());
                    String yaw = Integer.toString(Camera.getYaw());
                    this.imageName = tile_x + "_" + tile_y + "_" + tile_z + "_" + zoom + "_" 
                            + pitch + "_" + yaw;
                    Logger.log("Image File Name: " + this.imageName + ".png");
                    
                    // wait for 1/10 sec to make sure save frame is not painted
                    this.isSample = true;
                    Sleep.sleep(100);
                    
                    this.getEntities(); // take snapshot of entities in viewport
                    
                    // save image
                    SaveImage(this.imageName + ".png");
                    this.isSample = false;
                    
                    // get all annotations (BBs, name, UID, mask) from the game entities snapshot
                    Logger.log("Computing annotations...");
                    this.update_annotations(); 
                    
                    // save annotations [image file name] [UID] [name] [x] [y] [width] [height] [[x0,y0],[],...] []
                    Logger.log("Saving annotations...");
                    this.save_samples();
                    
                    // print annotation info
                    String num_npcs = Integer.toString(this.npcs_BBs.size());
                    String num_gos = Integer.toString(this.objects_BBs.size());
                    String num_players = Integer.toString(this.players_BBs.size());
                    String num_gis = Integer.toString(this.items_BBs.size());
                    Logger.log("Found #NPCs:" + num_npcs + " #GameObjects:" + num_gos + " #Players:" 
                            + num_players + " #GroundItems: " + num_gis);
                    
                }
                // reset count
                step_count = 0;
            }
        }
        return 0;
    }
    
    @Override
    public void onExit(){
        Logger.log("Done.");
    }
    
    @Override
    public void onPaint(Graphics g) { 
        if (!this.isSample && this.npcs!=null && this.objects!=null 
                && this.players!=null&& this.items!=null){ // don't draw annotations during save frames 
            Graphics2D g2D = (Graphics2D) g;
            
            // draw NPC annotations
            for (Entity entity: this.npcs){  
                Rectangle BB = entity.getBoundingBox();
                Model model = entity.getModel();
                if(BB != null && model != null){
                    g.setColor(Color.blue);
                    // draw boxes
                    g.drawRect(BB.x,BB.y,BB.width,BB.height);
                    // draw labels (name, UID)
                    g.drawString(entity.getName()+", "+Integer.toString(entity.getID()),BB.x,BB.y); 
                    // draw segmentation mask (list of pixel coordinates)
                    g2D.draw(model.calculateModelArea());  
                }
            }
            // draw Game object annotations
            for (Entity entity: this.objects){  
                Rectangle BB = entity.getBoundingBox();
                Model model = entity.getModel();
                if(BB != null && model != null){
                    g.setColor(Color.red);
                    g.drawRect(BB.x,BB.y,BB.width,BB.height);
                    g.drawString(entity.getName()+", "+Integer.toString(entity.getID()),BB.x,BB.y);
                    g2D.draw(model.calculateModelArea());  
                }
            }
            // draw Players annotations
            for (Entity entity: this.players){  
                Rectangle BB = entity.getBoundingBox();
                Model model = entity.getModel();
                if(BB != null && model != null){
                    g.setColor(Color.white);
                    g.drawRect(BB.x,BB.y,BB.width,BB.height);
                    g.drawString(entity.getName()+", "+Integer.toString(entity.getID()),BB.x,BB.y);
                    g2D.draw(model.calculateModelArea()); 
                }
            }
            // draw ground item annotations
            for (Entity entity: this.items){  
                Rectangle BB = entity.getBoundingBox();
                Model model = entity.getModel();
                if(BB != null && model != null){
                    g.setColor(Color.green);
                    g.drawRect(BB.x,BB.y,BB.width,BB.height);
                    g.drawString(entity.getName()+", "+Integer.toString(entity.getID()) + ", "+Integer.toString(entity.getRenderableHeight()),BB.x,BB.y);
                    g2D.draw(model.calculateModelArea()); 
                }
            }
        }  
    }

    public void RandomCameraView(){
        //int random_zoomlevel = (int)(Math.random()*1267) + 181; // random zoom level: 181(min zoom) - 1448(max zoom) 
        int random_zoomlevel = (int)(Math.random()*1048) + 400; // random zoom level: 400(min zoom) - 1448(max zoom)
        // limit max zoom out to reduce load
        int random_pitch = (int)(Math.random()*255) + 128; // random pitch: 128 - 383
        int random_yaw = (int)(Math.random()*2047); // random yaw: 0 - 2047
        Camera.setZoom(random_zoomlevel);
        Camera.rotateTo(random_yaw, random_pitch);     
    }

    public boolean FilterEntities(Entity e){ 
        // filter out entities too far or has no name (generic wall, grass, floor etc)
        return (e.distance(Players.getLocal())<=14 && !e.getName().equals("null")
                && !e.getName().equals("") && e.getBoundingBox()!=null);
    }
 
    public void getEntities(){
            this.npcs = NPCs.all(e -> FilterEntities(e));
            this.objects = GameObjects.all(e -> FilterEntities(e));
            this.players = Players.all(e -> FilterEntities(e));
            this.items = GroundItems.all(e -> FilterEntities(e));
    } 
    
    public void SaveImage(String filename){
        try {
            // retrieve image from game client
            BufferedImage sample_img = Client.getCanvasImage();
            // crop out game world 520x340
            sample_img = sample_img.getSubimage(0, 0, 520, 340);
            // save
            File outputfile = new File(this.root_dir + "images/" + filename);
            ImageIO.write(sample_img, "png", outputfile);
        } catch (IOException e) {
           Logger.log("Error: can't save image " + filename);
        }
    }
    
    public void update_annotations(){
        // Filter out entitites not in viewport
        //long t1 = System.nanoTime();
        Predicate<Entity> viewportcheck = e -> this.viewport.intersects(e.getBoundingBox()); 
        this.npcs = this.npcs.stream().filter(viewportcheck).collect(Collectors.toList());
        this.players = this.players.stream().filter(viewportcheck).collect(Collectors.toList());
        this.objects = this.objects.stream().filter(viewportcheck).collect(Collectors.toList());
        this.items = this.items.stream().filter(viewportcheck).collect(Collectors.toList());
        //long t2 = System.nanoTime();
        
        // clear all previously saved annotations
        this.npcs_BBs = new ArrayList<>();
        this.npcs_UIDs = new ArrayList<>();
        this.npcs_names = new ArrayList<>();
        this.npcs_masks = new ArrayList<>();
        this.players_BBs = new ArrayList<>();
        this.players_UIDs = new ArrayList<>();
        this.players_names = new ArrayList<>();
        this.players_masks = new ArrayList<>();
        this.objects_BBs = new ArrayList<>();
        this.objects_UIDs = new ArrayList<>();
        this.objects_names = new ArrayList<>();
        this.objects_masks = new ArrayList<>();
        this.items_BBs = new ArrayList<>();
        this.items_UIDs = new ArrayList<>();
        this.items_names = new ArrayList<>();
        this.items_masks = new ArrayList<>();
        //long t3 = System.nanoTime();
        
        // compute and save annotation snapshot
        for (NPC npc: this.npcs){
            if(npc.getBoundingBox() != null && npc.getModel() != null){ 
                this.npcs_BBs.add(npc.getBoundingBox());
                this.npcs_UIDs.add(npc.getID());
                this.npcs_names.add(npc.getName());
                this.npcs_masks.add(npc.getModel().calculateModelArea());
            }
        }
        for (Player player: this.players){
            if (player.getBoundingBox() != null && player.getModel() != null){
                this.players_BBs.add(player.getBoundingBox());
                this.players_UIDs.add(player.getID());
                this.players_names.add(player.getName());
                this.players_masks.add(player.getModel().calculateModelArea());
            }
        }
        for (GameObject object: this.objects){
            if (object.getBoundingBox() != null && object.getModel() != null){
                this.objects_BBs.add(object.getBoundingBox());
                this.objects_UIDs.add(object.getID());
                this.objects_names.add(object.getName());
                this.objects_masks.add(object.getModel().calculateModelArea());
            }
        }
        for (GroundItem item: this.items){
            if (item.getBoundingBox() != null && item.getModel() != null){
                this.items_BBs.add(item.getBoundingBox());
                this.items_UIDs.add(item.getID());
                this.items_names.add(item.getName());
                this.items_masks.add(item.getModel().calculateModelArea());
            }
        }
        //long t4 = System.nanoTime();
        
        //Logger.log("viewport filter:" + String.valueOf((t2-t1)/1000000) + " ms");
        //Logger.log("clearing vars:" + String.valueOf((t3-t2)/1000000) + " ms");
        //Logger.log("extracting annot:" + String.valueOf((t4-t3)/1000000) + " ms");
    }

    public void save_samples(){
        BufferedWriter npcs_buffer_w = null;
        BufferedWriter players_buffer_w = null;
        BufferedWriter objects_buffer_w = null;
        BufferedWriter items_buffer_w = null;
        try{
            FileWriter npcs_fw = new FileWriter(this.root_dir + this.npc_file, true);
            FileWriter players_fw = new FileWriter(this.root_dir + this.player_file, true);
            FileWriter objects_fw = new FileWriter(this.root_dir + this.go_file, true);
            FileWriter items_fw = new FileWriter(this.root_dir + this.item_file, true);
            
            npcs_buffer_w = new BufferedWriter(npcs_fw); //wrap
            players_buffer_w = new BufferedWriter(players_fw);
            objects_buffer_w = new BufferedWriter(objects_fw);
            items_buffer_w = new BufferedWriter(items_fw);

            for (int i = 0; i < this.npcs_BBs.size(); i++){
                String UID = Integer.toString(this.npcs_UIDs.get(i));
                String BB_x = Integer.toString(this.npcs_BBs.get(i).x);
                String BB_y = Integer.toString(this.npcs_BBs.get(i).y);
                String BB_w = Integer.toString(this.npcs_BBs.get(i).width);
                String BB_h = Integer.toString(this.npcs_BBs.get(i).height);
                String name = this.npcs_names.get(i).replace(" ", "_");
                String mask = this.AreaToStringPoints(this.npcs_masks.get(i));

                // [image name] [UID] [name] [x] [y] [width] [height] [[x0,y0],[x1,y1], ...]
                String Sample_line = this.imageName + ".png" + " " + UID + " " 
                        + name + " " + BB_x + " " + BB_y + " " + BB_w + " " + BB_h + " " + mask;
                // append annoation txt file with sample line
                npcs_buffer_w.write(Sample_line);
                npcs_buffer_w.newLine();
            }
            for (int i = 0; i < this.players_BBs.size(); i++){
                String UID = Integer.toString(this.players_UIDs.get(i));
                String BB_x = Integer.toString(this.players_BBs.get(i).x);
                String BB_y = Integer.toString(this.players_BBs.get(i).y);
                String BB_w = Integer.toString(this.players_BBs.get(i).width);
                String BB_h = Integer.toString(this.players_BBs.get(i).height);
                String name = this.players_names.get(i).replace(" ", "_");
                String mask = this.AreaToStringPoints(this.players_masks.get(i));
                // [image name] [entity ID] [entity name] [x] [y] [width] [height]
                String Sample_line = this.imageName + ".png" + " " + UID + " " 
                        + name + " " + BB_x + " " + BB_y + " " + BB_w + " " + BB_h + " " + mask;
                // append annoation txt file with sample line
                players_buffer_w.write(Sample_line);
                players_buffer_w.newLine();
            }
            for (int i = 0; i < this.objects_BBs.size(); i++){
                String UID = Integer.toString(this.objects_UIDs.get(i));
                String BB_x = Integer.toString(this.objects_BBs.get(i).x);
                String BB_y = Integer.toString(this.objects_BBs.get(i).y);
                String BB_w = Integer.toString(this.objects_BBs.get(i).width);
                String BB_h = Integer.toString(this.objects_BBs.get(i).height);
                String name = this.objects_names.get(i).replace(" ", "_");
                String mask = this.AreaToStringPoints(this.objects_masks.get(i));
                // [image name] [entity ID] [entity name] [x] [y] [width] [height]
                String Sample_line = this.imageName + ".png" + " " + UID + " " 
                        + name + " " + BB_x + " " + BB_y + " " + BB_w + " " + BB_h  + " " + mask;
                // append annoation txt file with sample line
                objects_buffer_w.write(Sample_line);
                objects_buffer_w.newLine();
            }
            for (int i = 0; i < this.items_BBs.size(); i++){
                String UID = Integer.toString(this.items_UIDs.get(i));
                String BB_x = Integer.toString(this.items_BBs.get(i).x);
                String BB_y = Integer.toString(this.items_BBs.get(i).y);
                String BB_w = Integer.toString(this.items_BBs.get(i).width);
                String BB_h = Integer.toString(this.items_BBs.get(i).height);
                String name = this.items_names.get(i).replace(" ", "_");
                String mask = this.AreaToStringPoints(this.items_masks.get(i));
                // [image name] [entity ID] [entity name] [x] [y] [width] [height]
                String Sample_line = this.imageName + ".png" + " " + UID + " " 
                        + name + " " + BB_x + " " + BB_y + " " + BB_w + " " + BB_h  + " " + mask;
                // append annoation txt file with sample line
                items_buffer_w.write(Sample_line);
                items_buffer_w.newLine();
            }

        } catch (IOException exc4){ 
            Logger.log(exc4.toString() + ": ERROR Writing to File");
        } finally{
            try{
                if (npcs_buffer_w != null){
                    npcs_buffer_w.close(); //close file
                }
                if (players_buffer_w != null){
                    players_buffer_w.close(); //close file
                }
                if (objects_buffer_w != null){
                    objects_buffer_w.close(); //close file
                }
                if (items_buffer_w != null){
                    items_buffer_w.close(); //close file
                }
             }
             catch (IOException exc5)
            {
                Logger.log("ERROR closing File");
            }
        }
    }
    
    public String AreaToStringPoints(Area area){ //return a string holding all the points coordinates
        String line = "";
        
        float[] coords = new float[6]; // currentSegment() needs a float/double arrary of lenght 6
        
        for (PathIterator piter = area.getPathIterator(null); !piter.isDone(); piter.next()) {
            int segment_type = piter.currentSegment(coords);

            switch(segment_type){
                case PathIterator.SEG_MOVETO:
                    line += "[[" + String.valueOf(coords[0]) + "," + String.valueOf(coords[1]) + "]";
                    break;
                case PathIterator.SEG_LINETO:
                    line += ",[" + String.valueOf(coords[0]) + "," + String.valueOf(coords[1]) + "]";
                    break;
                case PathIterator.SEG_CLOSE:
                    line += ",[" + String.valueOf(coords[0]) + "," + String.valueOf(coords[1]) + "]] ";
                    
            }
        
        }
        // [[seg_type, x, y], [], ...]
        return line;
    }
    
    // define a list of tiles for the bot to walk. ( https://explv.github.io )
    Tile[] path = {
        new Tile(3174, 3366, 0),
        new Tile(3174, 3366, 0),
        new Tile(3174, 3367, 0),
        new Tile(3175, 3367, 0),
        new Tile(3176, 3367, 0),
        new Tile(3176, 3366, 0),
        new Tile(3176, 3366, 0),
        new Tile(3176, 3367, 0),
        new Tile(3177, 3367, 0),
        new Tile(3178, 3367, 0)
    };
}