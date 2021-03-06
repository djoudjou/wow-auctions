package com.radcortez.wow.auctions.business;

import com.radcortez.wow.auctions.entity.*;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.*;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Roberto Cortez
 */
@SuppressWarnings({"unchecked"})
@Named
@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@ApplicationPath("/resources")
@Path("wowauctions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class WoWBusinessBean extends Application implements WoWBusiness {
    @PersistenceContext
    protected EntityManager em;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void createRealm(Realm realm) {
        em.persist(realm);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Realm updateRealm(Realm realm) {
        return em.merge(realm);
    }

    @Override
    @GET
    @Path("realms")
    public List<Realm> listRealms() {
        return em.createNamedQuery("Realm.listRealms").getResultList();
    }

    @Override
    public Realm findRealmById(Long realmId) {
        return em.find(Realm.class, realmId);
    }

    @Override
    public Optional<Realm> findRealmByNameOrSlug(String name, Realm.Region region) {
        Optional<Realm> realm;
        try {
            realm = Optional.of((Realm) em.createNamedQuery("Realm.findByNameOrSlugInRegion")
                                          .setParameter("name", name)
                                          .setParameter("slug", name)
                                          .setParameter("region", region)
                                          .getSingleResult());
        } catch (NoResultException e) {
            realm = Optional.empty();
        }
        return realm;
    }

    @Override
    public List<Realm> findRealmsByRegion(Realm.Region region) {
        return em.createNamedQuery("Realm.findByRegion").setParameter("region", region).getResultList();
    }

    @Override
    public boolean checkIfRealmExists(Realm realm) {
        return ((Long) em.createNamedQuery("Realm.exists")
                         .setParameter("name", realm.getName())
                         .setParameter("region", realm.getRegion())
                         .getSingleResult()) > 0;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void createRealmFolder(RealmFolder realmFolder) {
        em.persist(realmFolder);
    }

    @Override
    public RealmFolder findRealmFolderById(Long realmId, FolderType folderType) {
        return em.find(RealmFolder.class, new RealmFolder.RealmFolderPK(realmId, folderType));
    }

    @Override
    public boolean checkIfAuctionFileExists(AuctionFile auctionFile) {
        return ((Long) em.createNamedQuery("AuctionFile.exists")
                         .setParameter("url", auctionFile.getUrl())
                         .setParameter("lastModified", auctionFile.getLastModified())
                         .getSingleResult()) > 0;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void createAuctionFile(AuctionFile auctionFile) {
        em.persist(auctionFile);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public AuctionFile updateAuctionFile(AuctionFile auctionFile) {
        return em.merge(auctionFile);
    }

    @Override
    public List<AuctionFile> findAuctionFilesByRealmToProcess(Long realmId) {
        return em.createNamedQuery("AuctionFile.findByRealmAndFileStatus")
                 .setParameter("id", realmId)
                 .setParameter("fileStatus", FileStatus.LOADED)
                 .getResultList();
    }

    @Override
    public AuctionFile findAuctionFileById(Long auctionFileId) {
        return em.find(AuctionFile.class, auctionFileId);
    }

    @Override
    public List<Auction> findAuctionsByRealm(Long realmId, int start, int max) {
        return em.createNamedQuery("Auction.findByRealm")
                 .setParameter("realmId", realmId)
                 .setFirstResult(start)
                 .setMaxResults(max)
                 .getResultList();
    }

    @Override @GET
    @Path("items")
    public List<AuctionItemStatistics> findAuctionItemStatisticsByRealmAndItem(@QueryParam("realmId") Long realmId,
                                                                               @QueryParam("itemId") Integer itemId) {

        Realm realm = (Realm) em.createNamedQuery("Realm.findRealmsWithConnectionsById")
                                .setParameter("id", realmId)
                                .getSingleResult();

        // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=433075 if using EclipseLink
        List<Realm> connectedRealms = new ArrayList<>();
        connectedRealms.addAll(realm.getConnectedRealms());
        List<Long> ids = connectedRealms.stream().map(Realm::getId).collect(Collectors.toList());
        ids.add(realmId);

        return em.createNamedQuery("AuctionItemStatistics.findByRealmsAndItem")
                 .setParameter("realmIds", ids)
                 .setParameter("itemId", itemId)
                 .getResultList();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteAuctionDataByFile(Long fileId) {
        Query deleteQuery = em.createNamedQuery("Auction.deleteByAuctionFile");
        deleteQuery.setParameter("fileId", fileId);
        deleteQuery.executeUpdate();
    }
}
