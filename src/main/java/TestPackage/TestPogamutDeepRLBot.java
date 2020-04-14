package TestPackage;
import DeepRLPackage.PogamutPackage.AddNativeBot;
import DeepRLPackage.PogamutPackage.PogamutGameState;
import DeepRLPackage.DeepRLBrain;
import static DeepRLPackage.PogamutPackage.PogamutDeepRLBot.DEAL_DAMAGE_REWARD_MODIFIER;
import static DeepRLPackage.PogamutPackage.PogamutDeepRLBot.DEATH_REWARD;
import static DeepRLPackage.PogamutPackage.PogamutDeepRLBot.FRAG_REWARD;
import static DeepRLPackage.PogamutPackage.PogamutDeepRLBot.LIVING_REWARD;
import static DeepRLPackage.PogamutPackage.PogamutDeepRLBot.TAKE_DAMAGE_REWARD_MODIFIER;
import static com.oracle.jrockit.jfr.ContentType.Timestamp;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.EventListener;
import java.util.List;
import java.util.Arrays;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.base3d.worldview.object.Rotation;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004Bot;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004BotModuleController;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotDamaged;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotKilled;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.PlayerDamaged;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.PlayerKilled;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.ItemPickedUp;        
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Move;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.utils.exception.PogamutException;
import java.io.File;
import java.sql.Timestamp;
import java.util.Date;



////// New GameState and changed rewards
@AgentScoped
public class TestPogamutDeepRLBot extends UT2004BotModuleController {

    public static final List<String> Actions = Arrays.asList(
            /*Name of file containing functions that trigger actions*/
            /*AdvancedLocomotion*/
           "strafeRight", "strafeLeft", "doubleJump", "jump", "turnToEnemy",
          "RunTowardsNearestAmmo", "RunTowardsNearestHealthItem", "ChaseEnemy",            
           /* AdvancedShooting*/ 
          "shootPrimaryAtEnemy",
            /*Custom*/
          "MoveForwards","MoveBackwards","MoveLeft","MoveRight"
    );
    private DeepRLBrain qLearningAgent; 
    private String action,previousAction = "";
    private PogamutGameState state;    
    private double reward;
    public double totalReward;
    private int counter = 100;
    
    @Override
    public void prepareBot(UT2004Bot bot) {  
        //<editor-fold defaultstate="collapsed" desc="Init and Find Newest File">
        boolean noFilesFound = false;
        File lastModifiedFile = null;
        File dir = new File("C:\\Users\\Anton\\Desktop\\C-Uppsatts\\Projects\\DeepRLProject\\NewTrainingData\\");
        File[] files = dir.listFiles();
        if(files.length != 0){
            lastModifiedFile = files[0];
            for (int i = 1; i < files.length; i++) {
                if (lastModifiedFile.lastModified() < files[i].lastModified()) {
                    lastModifiedFile = files[i];
                }
            }
        }
        //</editor-fold>
       qLearningAgent = new DeepRLBrain(lastModifiedFile,0.0,PogamutGameState.UT2004NrOfFeatures,Actions.size(),"C:\\Users\\Anton\\Desktop\\C-Uppsatts\\Projects\\DeepRLProject\\NewTrainingData\\");
        
        
    }

    @Override
    public void beforeFirstLogic() {
        move.setRotationSpeed(new Rotation(4608,90000 , 3072));
        reward = 0;
        totalReward = 0;
    }
    
    @Override
    public void logic() throws PogamutException {
        if(previousAction.equals("shootPrimaryAtEnemy")){
            shoot.stopShooting();
            previousAction = "";
        }
        else{
            state = new PogamutGameState(info, players,items,senses);
            action = qLearningAgent.getPolicy(state, Actions);                
            performAction(action);    
            previousAction = action;
            //System.out.println(action);
        }        
        
        totalReward += reward;        
        counter++;
        if(counter >= 100){
            System.out.println(new Timestamp(System.currentTimeMillis()) + "Total Reward: " + totalReward);
            counter = 0;
        }        
        reward = LIVING_REWARD;
        
    }
    
    private void performAction(String action) {
        try {
          
            //<editor-fold defaultstate="collapsed" desc="Dodging">
            //else if(action.equals("moveContinuos"))
            // move.moveContinuos();
            if (action.equals("strafeRight")) {
                move.strafeRight(100);
            } else if (action.equals("strafeLeft")) {
                move.strafeLeft(100);
            } else if (action.equals("doubleJump")) {
                move.doubleJump();
            } else if (action.equals("jump")) {
                move.jump();
            }
            
            //</editor-fold>            
            //<editor-fold defaultstate="collapsed" desc="Looking (turnTo)">            
            else if (action.equals("turnToEnemy")) {
                if(players.canSeeEnemies())
                    move.turnTo(players.getNearestVisibleEnemy());
            } //</editor-fold>
            //<editor-fold defaultstate="collapsed" desc="Run To Target">
            
            else if (action.equals("RunTowardsNearestAmmo") && items.getPathNearestSpawnedItem(ItemType.Category.AMMO) != null) {
                navigation.navigate(items.getPathNearestSpawnedItem(ItemType.Category.AMMO));
            } 
            else if (action.equals("RunTowardsNearestHealthItem") && items.getPathNearestSpawnedItem(ItemType.Category.HEALTH) != null) {
                navigation.navigate(items.getPathNearestSpawnedItem(ItemType.Category.HEALTH));
            } 
           
            else if (action.equals("ChaseEnemy")) {
                if (players.canSeeEnemies()) {
                    navigation.navigate(players.getNearestVisibleEnemy());
                }
                else if(players.getNearestEnemy(Double.MAX_VALUE) != null){
                    navigation.navigate(players.getNearestEnemy(Double.MAX_VALUE));
                }
                
            }
            //</editor-fold>            
            //<editor-fold defaultstate="collapsed" desc="Directional Movement">               
            else if (action.equals("MoveForwards")) {             
                move.moveTo(info.getLocation().add(info.getRotation().toLocation().scale(119)));
            }
            else if (action.equals("MoveBackwards")) {             
                getAct().act(
                    new Move()
                            .setFirstLocation(info.getLocation().sub(info.getRotation().toLocation().scale(119)))
                            .setFocusLocation(info.getLocation()));            
            }
            else if (action.equals("MoveLeft")) {             
                move.moveTo(info.getLocation().add(info.getRotation().toLocation().cross(new Location(new double[]{0,0,-1}).getNormalized().scale(119))));
            }
            else if (action.equals("MoveRight")) {             
                move.moveTo(info.getLocation().add(info.getRotation().toLocation().cross(new Location(new double[]{0,0,1}).getNormalized().scale(119))));
            }
            //</editor-fold>            
            //<editor-fold defaultstate="collapsed" desc="ShootCommands">
            else if (action.equals("shootPrimaryAtEnemy")) {
                if (players.canSeeEnemies()) {
                    shoot.shoot(players.getNearestVisibleEnemy());                    
                }
            }
//</editor-fold>

        } //Sometimes, some actions are unavailable. At which point, nullpointerException is Thrown.
        catch (Exception e) {
            System.out.println("EXCEPTION: " + e.toString() + ". During action: "+action);
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Reward Functions and Listeners">    
    @Override
    public void botKilled(BotKilled event) {
        reward = DEATH_REWARD;
    }
    
    @EventListener(eventClass = PlayerKilled.class)
    public void playerKilled(PlayerKilled event) {
        reward = FRAG_REWARD;     
        System.err.println("!!!!!AGENT KILLED ENEMY!!!!!");
    }
    
    @EventListener(eventClass = PlayerDamaged.class)
    public void playerDamaged(PlayerDamaged event) {
        reward += (double) event.getDamage() * DEAL_DAMAGE_REWARD_MODIFIER;
    }
    
    @EventListener(eventClass = BotDamaged.class)
    public void botDamaged(BotDamaged event) {
        reward += (double) event.getDamage() * TAKE_DAMAGE_REWARD_MODIFIER;
    }
    
    @EventListener(eventClass = ItemPickedUp.class)
    public void itemPickedUp(ItemPickedUp event) {
        if (event.getType().getCategory() == ItemType.Category.HEALTH) {
            double r = ((200 - info.getHealth())) * event.getAmount(); // MAX = 0.009950 MIN = 0
            reward += r * 0.000002;
        } else if (event.getType().getCategory() == ItemType.Category.AMMO
                || event.getType().getCategory() == ItemType.Category.WEAPON) {
            double r = ((30 - info.getCurrentAmmo())) * event.getAmount(); // MAX = 0.00072 MIN = 0            
            reward += r * 0.000002;
        }
    }
        
//</editor-fold>
    
    public static void main(String args[]) throws PogamutException {        
        UT2004BotRunner ut = new UT2004BotRunner(TestPogamutDeepRLBot.class,"Opponent1").setMain(true);                   
        ut.startAgent();           
       
    }
}
