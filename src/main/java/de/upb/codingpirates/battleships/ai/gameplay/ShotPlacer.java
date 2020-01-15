package de.upb.codingpirates.battleships.ai.gameplay;

import com.google.common.collect.Lists;
import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.ai.logger.MARKER;
import de.upb.codingpirates.battleships.ai.util.*;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.Shot;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Implements the 3 possible shot methods with difficulty levels 1, 2 or 3.
 */
public class ShotPlacer {
    private static final Logger logger = LogManager.getLogger();
    Ai ai;

    public ShotPlacer(Ai ai) {
        this.ai = ai;
    }

    //difficulty level 1

    /**
     * Places shots randomly on the field of one opponent and sends the {@link ShotsRequest} message.
     * <p>
     * Difficulty level 1.
     *
     * @return shots
     */
    public Collection<Shot> placeShots_1(int shotCount) {
        RandomPointCreator randomPointCreator = new RandomPointCreator(this.ai);
        int numberOfClients = ai.getClientArrayList().size();
        int shotClientId;
        int aiIndex = -1;

        for (Client c : ai.getClientArrayList()) {
            if (c.getId() == ai.getAiClientId()) {
                aiIndex = ai.getClientArrayList().indexOf(c);
            }
        }

        while (true) {
            int randomIndex = (int) (Math.random() * numberOfClients);
            if (randomIndex != aiIndex) {
                shotClientId = ai.getClientArrayList().get(randomIndex).getId(); //shotClientId is the target for placing shots in the next part
                logger.info(MARKER.Ai_ShotPlacer, "Shooting on client with id: {} ", shotClientId);
                break;
            }
        }

        ArrayList<Shot> myShots = new ArrayList<>();

        //placing the shots randomly until the max of shots is not reached
        //all shots will be placed on the field of only one opponents field(other client)
        int i = 0;
        while (i < shotCount) {
            logger.info(MARKER.Ai_ShotPlacer, "Trying to find  {}. shot this round", i + 1);

            Point2D aimPoint = randomPointCreator.getRandomPoint2D();

            Shot targetShot = new Shot(shotClientId, aimPoint);
            boolean alreadyChoosen = false;


            for (Shot s : ai.getRequestedShots()) {
                if (PositionComparator.compareShots(s, targetShot)) {
                    alreadyChoosen = true;
                    logger.info(MARKER.Ai_ShotPlacer, "Shot was requested already: {}", targetShot);
                    break;
                }
            }
            if (alreadyChoosen) continue;

            for (Shot s : ai.getHits()) {
                if (PositionComparator.compareShots(s, targetShot)) {
                    alreadyChoosen = true;
                    logger.info(MARKER.Ai_ShotPlacer, "Shot is a hit already: {}", targetShot);
                    break;
                }
            }
            if (alreadyChoosen) continue;

            for (Shot s : ai.getMisses()) {
                if (PositionComparator.compareShots(s, targetShot)) {
                    alreadyChoosen = true;
                    logger.info(MARKER.Ai_ShotPlacer, "Shot is a miss already: {}", s);
                    break;
                }
            }
            if (alreadyChoosen) continue;

            //used only when called by placeShots_2

            for (Shot s : pot) {
                if (PositionComparator.compareShots(s, targetShot)) {
                    alreadyChoosen = true;
                    logger.info(MARKER.Ai_ShotPlacer, "Shot should be used already: {}", s);
                }
            }

            myShots.add(targetShot);
            ai.requestedShots.add(targetShot);
            logger.info(MARKER.Ai_ShotPlacer, "Found shot {}", targetShot);
            i++;

        }

        //return the choosen shots
        logger.info("My shots: {}", myShots);
        return myShots;
    }


    //difficulty level 2 -----------------------------------------------------------------


    Set<Shot> pot = new HashSet<>(); //the points which could be hits
    /**
     * Places shots using the hunt and target algorithm.
     * Difficulty level 2.
     *
     * @return requested shots
     */
    public Collection<Shot> placeShots_2() {
        logger.info(MARKER.Ai_ShotPlacer, "Placing shots with difficulty level 2");
        RandomPointCreator randomPointCreator = new RandomPointCreator(this.ai);
        Collection<Shot> myShots = new ArrayList<>();
        //ai.addMisses();


        int aiIndex;
        for (Client c : ai.getClientArrayList()) {
            if (c.getId() == ai.getAiClientId()) {
                aiIndex = ai.getClientArrayList().indexOf(c);
                logger.info(MARKER.Ai_ShotPlacer, "Ai Index: {}.", aiIndex);
            }
        }


        if (ai.getHits().isEmpty()) {
            placeShots_1(ai.getShotCount()); // if no hits exists call random shot method and place shot randomly
        } else {
            logger.debug("Size hits");
            for (Shot s : ai.getHits()) {
                if (s.getClientId() != ai.getAiClientId()) {
                    logger.info(MARKER.Ai_ShotPlacer, "Looking for all neighbours of Shot {}", s);

                    int id = s.getClientId();
                    ArrayList<Shot> temp = new ArrayList<>();
                    //west
                    temp.add(new Shot(id, new Point2D(s.getTargetField().getX() - 1, s.getTargetField().getY())));
                    //south
                    temp.add(new Shot(id, new Point2D(s.getTargetField().getX(), s.getTargetField().getY() - 1)));
                    //east
                    temp.add(new Shot(id, new Point2D(s.getTargetField().getX() + 1, s.getTargetField().getY())));
                    //north
                    temp.add(new Shot(id, new Point2D(s.getTargetField().getX(), s.getTargetField().getY() + 1)));

                    boolean isHitOrMiss = false;

                    for (Shot p : temp) {
                        logger.info("Looking at point {}", p);
                        if (p.getTargetField().getX() >= 0 & p.getTargetField().getY() >= 0) {
                            logger.debug("{} is in field", p);
                            for (Shot h : ai.getHits()) {
                                if (PositionComparator.compareShots(p, h)) {
                                    logger.debug("{} is a hit", p);
                                    isHitOrMiss = true;
                                    break;
                                }

                            }
                            if (isHitOrMiss) continue;
                            for (Shot h : ai.getMisses()) {
                                if (PositionComparator.compareShots(p, h)) {
                                    logger.debug("{} is a miss", p);
                                    isHitOrMiss = true;
                                    break;
                                }
                            }
                            if (isHitOrMiss) continue;
                        } else {
                            continue;
                        }
                        logger.debug("Add point {}", p);
                        pot.add(p);
                        logger.info(MARKER.Ai_ShotPlacer, "Added {} to potential hits", p);
                    }
                }
                if (pot.size() < ai.getShotCount()) {
                    logger.info(MARKER.Ai_ShotPlacer, "There are less potential hits ({}) as possible shots ({})", pot.size(), ai.getShotCount());
                    for (Shot p : pot) {
                        myShots.add(p);
                        logger.info(MARKER.Ai_ShotPlacer, "Added {} to myShots", p);
                    }
                    int availableShots = ai.getShotCount() - myShots.size();
                    myShots.addAll(placeShots_1(availableShots));
                } else {
                    logger.info(MARKER.Ai_ShotPlacer, "There are more potential hits ({}) as possible shots ({})", pot.size(), ai.getShotCount());
                    for (Shot p : pot) {
                        myShots.add(p);
                        logger.info(MARKER.Ai_ShotPlacer, "Added {} to myShots", p);
                        if (myShots.size() == ai.getShotCount()) break;
                    }
                }
                if (myShots.size() == ai.getShotCount()) break;
            }
        }

        pot.clear();
        ai.requestedShotsLastRound.clear();
        ai.requestedShotsLastRound.addAll(myShots);
        return myShots;

    }


    //difficulty level 3 ---------------------------------------------------------


    /**
     * Placing shots based on the relative value. Heatmaps will be created with relative values.
     *
     * @return
     */
    public Collection<Shot> placeShots_Relative_3() {

        for (Client c : ai.getClientArrayList()) {
            InvalidPointsHandler invalidPointsHandler = new InvalidPointsHandler(this.ai);
            LinkedList<Point2D> inv = invalidPointsHandler.createInvalidPointsOne(c.getId());
            ai.addPointsToInvalid(inv, c.getId());
        }

        HeatmapCreator heatmapCreator = new HeatmapCreator(this.ai);
        ai.setHeatmapAllClients(heatmapCreator.createHeatmapAllClients());
        //valid targets are the clients which are connected and can be targets for firing shots
        Map<Integer, LinkedList<Integer>> validTargets = new HashMap<>();
        //search for valid targets: all clients which are not this ai and have ships and have less invalid points than the field has points
        for (Map.Entry<Integer, LinkedList<Integer>> entry : ai.getAllSunkenShipIds().entrySet()) {
            if (!(ai.getInvalidPointsAll().get(entry.getKey()).size() == (ai.getWidth() * ai.getHeight())
                    | entry.getValue().size() == ai.getShips().size() | entry.getKey() == ai.getAiClientId())) {
                validTargets.put(entry.getKey(), entry.getValue());
            }
        }


        //allHeatVal is the collection of all valid points of all clients and those heat value
        LinkedList<Triple<Integer, Point2D, Double>> allHeatVal = Lists.newLinkedList();

        //using the class Triple store the triple in allHeatVal if the target is valid
        //for use of class Triple see the nested for loops
        for (Map.Entry<Integer, Double[][]> entry : ai.getHeatmapAllClients().entrySet()) {
            int clientId = entry.getKey();
            if (!validTargets.containsKey(clientId)) continue;
            for (int i = 0; i < entry.getValue().length; i++) {
                for (int j = 0; j < entry.getValue()[i].length; j++) {
                    //Triple is used like that to order and store the values of each heat point
                    Triple<Integer, Point2D, Double> t = new Triple<>(clientId, new Point2D(j, i), entry.getValue()[i][j]);
                    allHeatVal.add(t);
                }
            }
        }

        //remove all zero values of the heat points collection
        allHeatVal.removeIf((Triple<Integer, Point2D, Double> t) -> t.getVal3() == (double) 0);

        //using the TripleComparator class we can sort the the triple objects by their heat value
        allHeatVal.sort(new TripleComparator().reversed());


        ai.setAllHeatVal(allHeatVal);


        //all shots which will be fired this round
        Collection<Shot> myShotsThisRound = Lists.newArrayList();

        boolean valid;

        for (Triple<Integer, Point2D, Double> t : allHeatVal) {

            boolean isHit;

            for (Shot s : ai.getRequestedShots()) {
                isHit = false;
                for (Shot r : ai.getHits()) {
                    if (PositionComparator.compareShots(s, r)) {
                        isHit = true;
                        break;
                    }
                }
                if (!isHit) {
                    ai.addPointsToInvalid(s.getTargetField(), s.getClientId());
                    ai.getMisses().add(s);
                }
            }

            valid = true;

            int clientId = t.getVal1(); //client id
            Point2D p = t.getVal2(); //heat point
            double fieldVal = t.getVal3(); //heat value

            for (Point2D g : ai.getInvalidPointsAll().get(clientId)) {
                if (g.getX() == p.getX() & g.getY() == p.getY()) {
                    logger.debug(MARKER.Ai_ShotPlacer, "If this block is called something went wrong. {} is invalid", t);
                    valid = false;
                    break;
                }
            }
            for (Shot k : ai.getHits()) {
                if (k.getTargetField().getX() == p.getX() & k.getTargetField().getY() == p.getY() & k.getClientId() == clientId) {
                    valid = false;
                    break;
                }
            }
            if (!valid) continue;

            Shot targetShot = new Shot(clientId, p);
            myShotsThisRound.add(targetShot);
            logger.info(MARKER.Ai_ShotPlacer, "Added shot {} with value {}", targetShot, fieldVal);
            if (myShotsThisRound.size() >= ai.getShotCount()) {
                break;
            }
        }
        ai.requestedShots.addAll(myShotsThisRound);

        return myShotsThisRound;


    }
}
