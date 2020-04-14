package DeepRLPackage.PogamutPackage;

import DeepRLPackage.IGameState;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.AgentInfo;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.Items;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.Players;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.Senses;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.IncomingProjectile;
import javax.vecmath.Vector3d;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;



/**
 *
 * @author Anton
 */
public class PogamutGameState implements IGameState {
    public static final int UT2004NrOfFeatures = 61;
    
    private INDArray state;    
    public PogamutGameState(AgentInfo data,Players players, Items items,Senses senses){        
        
        
        
        //<editor-fold defaultstate="collapsed" desc="DICHOTOMOUS ATTRIBUTES">
        double hasArmor = 0;
        if(data.hasArmor())
            hasArmor = 1;
        double hasFastFire = 0;
        if(data.hasFastFire())
            hasFastFire = 1;
        double hasHighArmor = 0;
        if(data.hasHighArmor())
            hasHighArmor = 1;
        double hasLowArmor = 0;
        if(data.hasLowArmor())
            hasLowArmor = 1;
        double hasRegeneration = 0;
        if(data.hasRegeneration())
            hasRegeneration = 1;
        double hasUDamage = 0;
        if(data.hasUDamage())
            hasUDamage = 1;
        double hasWeapon = 0;
        if(data.hasWeapon())
            hasWeapon = 1;
        double isAdrenalineFull = 0;
        if(data.isAdrenalineFull())
            isAdrenalineFull = 1;
        double isAdrenalineSufficient = 0;
        if(data.isAdrenalineSufficient())
            isAdrenalineSufficient = 1;
        double isCrouched = 0;
        if(data.isCrouched())
            isCrouched = 1;
        double isHealthy = 0;
        if(data.isHealthy())
            isHealthy = 1;
        double isMoving = 0;
        if(data.isMoving())
            isMoving = 1;
        double isPrimaryShooting = 0;
        if(data.isPrimaryShooting())
            isPrimaryShooting = 1;
        double isSecondaryShooting = 0;
        if(data.isSecondaryShooting())
            isSecondaryShooting = 1;
        double isSuperHealthy = 0;
        if(data.isSuperHealthy())
            isSuperHealthy = 1;
        double isTouchingGround = 0;
        if(data.isTouchingGround())
            isTouchingGround = 1;
        double canSeeEnemy = 0;
        if(players.canSeeEnemies())
            canSeeEnemy = 1;  
        
        double isBeingDamaged = 0;
        if(senses.isBeingDamagedOnce())
            isBeingDamaged = 1;
        double isCausingDamage = 0;
        if(senses.isCausingDamageOnce())
            isCausingDamage = 1;
        double isColliding = 0;
        if(senses.isCollidingOnce())
            isColliding = 1;
        double isFalling = 0;
        if(senses.isFallEdgeOnce())
            isFalling = 1;
        double isHearingNoise = 0;
        if(senses.isHearingNoiseOnce())
            isHearingNoise = 1;
        double isHearingPickup = 0;
        if(senses.isHearingPickupOnce())
            isHearingPickup = 1;
        double isItemPickedUp = 0;
        if(senses.isItemPickedUpOnce())
            isItemPickedUp = 1;
        //</editor-fold>
        
        //<editor-fold defaultstate="collapsed" desc="Resources and location">
        double adrenaline = data.getAdrenaline() / (100);
        double armor = data.getArmor() / (150);
        double primAmmo = data.getCurrentAmmo() / 30;
        
        double health = data.getHealth() / 200;
        double highArmor = data.getHighArmor() / 100;
        double lowArmor = data.getLowArmor() / 50;
        
        // Formula used for normalization is: v - min(v) / max(v) - min(v)
        double floorLocationX = (data.getFloorLocation().x - 800.0) / (3191.0 - 800.0); //minX: 800 maxX: 3191
        double floorLocationY = (data.getFloorLocation().y + 2595.0) / (676.0 + 2595.0);// minY: -2595 maxY: 676
        double floorLocationZ = (data.getFloorLocation().z + 420.0) / (100.0 + 420.0); // minZ: -420 maxZ: 100
        
//</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="Items">
        double nearestItemX = 1;
        double nearestItemY = 1;
        double nearestItemZ = 1;
        double nearestItemType = 1;
        double distNearestItem = 1;
        double distNearestHealthItem = 1;
        double distNearestAmmoItem = 1;
        double distNearestAmmoWeapon = 1;
        double distNearestAdrenalineItem = 1;
        
        if (items.getNearestSpawnedItem() != null) {
            if (items.getNearestSpawnedItem() != null) {
                nearestItemX = (items.getNearestSpawnedItem().getLocation().x - 800.0) / (3191.0 - 800.0);
                nearestItemY = (items.getNearestSpawnedItem().getLocation().y + 2595.0) / (676.0 + 2595.0);
                nearestItemZ = (items.getNearestSpawnedItem().getLocation().z + 420.0) / (100.0 + 420.0);
            }
            
            nearestItemType = 0.25;///Represents ADRENALINE
            if (items.getNearestSpawnedItem().getType().getCategory() == ItemType.Category.AMMO
                    || items.getNearestSpawnedItem().getType().getCategory() == ItemType.Category.WEAPON) {
                nearestItemType = 0.5;
            } else if (items.getNearestSpawnedItem().getType().getCategory() == ItemType.Category.HEALTH) {
                nearestItemType = 0.75;
            }
            
            distNearestItem = data.getDistance(items.getNearestSpawnedItem().getLocation()) / 4035; // Euclidean distance based on the values used with floorLocation (a^2 = b^2 + c^2)
            
            if (items.getNearestSpawnedItem(ItemType.Category.HEALTH) != null) {
                distNearestHealthItem = items.getNearestSpawnedItem(ItemType.Category.HEALTH).getLocation().getDistance(data.getLocation()) / 4035;
            }
            
            if (items.getNearestSpawnedItem(ItemType.Category.AMMO) != null) {
                distNearestAmmoItem = items.getNearestSpawnedItem(ItemType.Category.AMMO).getLocation().getDistance(data.getLocation()) / 4035;
            }
            
            if (items.getNearestSpawnedItem(ItemType.Category.WEAPON) != null) {
                distNearestAmmoWeapon = items.getNearestSpawnedItem(ItemType.Category.WEAPON).getLocation().getDistance(data.getLocation())/ 4035;
            }
            
            if (items.getNearestSpawnedItem(ItemType.Category.ADRENALINE) != null) {
                distNearestAdrenalineItem = items.getNearestSpawnedItem(ItemType.Category.ADRENALINE).getLocation().getDistance(data.getLocation()) / 4035;
            }
        }
        //</editor-fold>
        
        //<editor-fold defaultstate="collapsed" desc="Distance enemy and noiseRotation">
        double distNearestEnemy = 1;
        double enemyX = 1;
        double enemyY = 1;
        double enemyZ = 1;
        double isFiring = 0;
        if(players.canSeeEnemies()){
            distNearestEnemy = data.getDistance(players.getNearestVisibleEnemy().getLocation()) / 4035; // Max distance
            enemyX = (players.getNearestVisibleEnemy().getLocation().x - 800.0) / (3201.0 - 800.0); //minX: 800 maxX: 3191, offset for when not hearing noises
            enemyY = (players.getNearestVisibleEnemy().getLocation().y + 2595.0) / (680.0 + 2595.0);// minY: -2595 maxY: 676, offset for when not hearing noises
            enemyZ = (players.getNearestVisibleEnemy().getLocation().z + 420.0) / (110.0 + 420.0); // minZ: -420 maxZ: 100, offset for when not hearing noises
            isFiring = players.getNearestVisibleEnemy().getFiring() / 2;
        }
        
        double noiseRotX = 1;
        double noiseRotZ = 1;
        double noiseRotY = 1;
        if(isHearingNoise == 1){
            noiseRotX = senses.getNoiseRotation().pitch / 65540; //(senses.getNoiseRotation().pitch + 32768) / (32800 +32768); //min: -32768 max: 32767, offset for when not hearing noises            
            noiseRotY = senses.getNoiseRotation().yaw / 65540;//(senses.getNoiseRotation().yaw + 32768) / (32800 +32768); //min: -32768 max: 32767, offset for when not hearing noises
            noiseRotZ = senses.getNoiseRotation().roll / 65540;//(senses.getNoiseRotation().roll + 32768) / (32800 +32768); //min: -32768 max: 32767, offset for when not hearing noises
            
            if(noiseRotX > 1 || noiseRotX < 0)
                System.out.println("}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}"+noiseRotX);
            if(noiseRotZ > 1 || noiseRotZ < 0)
                System.out.println("}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}"+noiseRotZ);
            if(noiseRotY > 1 || noiseRotY < 0)
                System.out.println("}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}"+noiseRotY);
        }
        
//</editor-fold>
        
        //<editor-fold defaultstate="collapsed" desc="Incoming Projectile">
        
        double rocketDamageRadious = 0;
        double rocketImpactTime = 1;
        double rocketX = 1;
        double rocketY = 1;
        double rocketZ = 1;
        double rocketOriginX = 1;
        double rocketOriginY = 1;
        double rocketOriginZ = 1;
        double rocketDirectionX = 1;
        double rocketDirectionY = 1;
        double rocketDirectionZ = 1;
        if(senses.seeIncomingProjectileOnce()){
            IncomingProjectile enemyRocketInfo = senses.getLastIncomingProjectile();
            
            rocketDamageRadious = enemyRocketInfo.getDamageRadius() / 220;
            rocketImpactTime = enemyRocketInfo.getImpactTime() / 4; // Longest posible airtime is 4 seconds
            rocketX = (enemyRocketInfo.getLocation().x - 800.0) / (3191.0 - 800.0); //minX: 800 maxX: 3191
            rocketY = (enemyRocketInfo.getLocation().y + 2595.0) / (676.0 + 2595.0);// minY: -2595 maxY: 676
            rocketZ = (enemyRocketInfo.getLocation().z + 420.0) / (100.0 + 420.0); // minZ: -420 maxZ: 100
            rocketOriginX = (enemyRocketInfo.getOrigin().x - 800.0) / (3191.0 - 800.0); //minX: 800 maxX: 3191
            rocketOriginY = (enemyRocketInfo.getOrigin().y + 2595.0) / (676.0 + 2595.0);// minY: -2595 maxY: 676
            rocketOriginZ = (enemyRocketInfo.getOrigin().z + 420.0) / (100.0 + 420.0); // minZ: -420 maxZ: 100
            
            Vector3d dir = enemyRocketInfo.getDirection();
            dir.clamp(0.0, 1.0);
            rocketDirectionX = dir.x;
            rocketDirectionY = dir.y;
            rocketDirectionZ = dir.z;
        }
        
//</editor-fold>

        state = Nd4j.create(new double[]
            {                
                hasArmor, 
                hasFastFire,
                hasHighArmor,
                hasLowArmor,
                hasRegeneration,
                hasUDamage,
                hasWeapon,
                isAdrenalineFull,
                isAdrenalineSufficient,
                isCrouched,
                isHealthy,
                isMoving,
                isPrimaryShooting,
                isSecondaryShooting,
                isSuperHealthy,
                isTouchingGround,
                canSeeEnemy,
                adrenaline,
                armor,
                primAmmo,
                health,
                highArmor,
                lowArmor,
                floorLocationX,
                floorLocationY,
                floorLocationZ,
                nearestItemX,
                nearestItemY,
                nearestItemZ,
                distNearestItem,
                distNearestEnemy,
                nearestItemType,
                distNearestHealthItem,
                distNearestAmmoItem,
                distNearestAdrenalineItem,
                distNearestAmmoWeapon, 
                enemyX,
                enemyY,
                enemyZ,
                isFiring,
                rocketDamageRadious,
                rocketImpactTime,
                rocketX,
                rocketY,
                rocketZ,
                rocketOriginX,
                rocketOriginY,
                rocketOriginZ,
                rocketDirectionX,
                rocketDirectionY,
                rocketDirectionZ, 
                isBeingDamaged,
                isCausingDamage,
                isColliding,
                isFalling,
                isHearingNoise,
                isHearingPickup,
                isItemPickedUp,
                noiseRotX,
                noiseRotZ,
                noiseRotY, //61
            }
        );              
    }    
    public INDArray getState(){
        return state;
    }
}