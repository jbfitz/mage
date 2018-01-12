/*
 *  Copyright 2010 BetaSteward_at_googlemail.com. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification, are
 *  permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright notice, this list of
 *        conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright notice, this list
 *        of conditions and the following disclaimer in the documentation and/or other materials
 *        provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY BetaSteward_at_googlemail.com ``AS IS'' AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL BetaSteward_at_googlemail.com OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *  ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  The views and conclusions contained in the software and documentation are those of the
 *  authors and should not be interpreted as representing official policies, either expressed
 *  or implied, of BetaSteward_at_googlemail.com.
 */
package mage.cards.r;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import mage.ObjectColor;
import mage.abilities.Ability;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.costs.common.SacrificeTargetCost;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.CantBeTargetedSourceEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.filter.FilterObject;
import mage.filter.FilterStackObject;
import mage.filter.FilterPermanent;
import mage.filter.common.FilterControlledCreaturePermanent;
import mage.filter.predicate.Predicates;
import mage.filter.predicate.mageobject.ColorPredicate;
import mage.filter.predicate.mageobject.SubtypePredicate;
import mage.filter.predicate.permanent.TappedPredicate;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.Target;
import mage.target.TargetPermanent;
import mage.target.common.TargetControlledCreaturePermanent;

/**
 *
 * @author L_J
 */
public class RaidingParty extends CardImpl {
    
    private static final FilterObject filterWhite = new FilterStackObject("white spells or abilities from white sources");
    private static final FilterControlledCreaturePermanent filterOrc = new FilterControlledCreaturePermanent("an Orc");
    
    static {
        filterWhite.add(new ColorPredicate(ObjectColor.WHITE));
        filterOrc.add(new SubtypePredicate(SubType.ORC));
    }

    public RaidingParty(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId,setInfo,new CardType[]{CardType.ENCHANTMENT},"{2}{R}");

        // Raiding Party can't be the target of white spells or abilities from white sources.
        this.addAbility(new SimpleStaticAbility(Zone.BATTLEFIELD, new CantBeTargetedSourceEffect(filterWhite, Duration.WhileOnBattlefield)));
        
        // Sacrifice an Orc: Each player may tap any number of untapped white creatures he or she controls. For each creature tapped this way, that player chooses up to two Plains. Then destroy all Plains that weren't chosen this way by any player.
        this.addAbility(new SimpleActivatedAbility(Zone.BATTLEFIELD, new RaidingPartyEffect(), new SacrificeTargetCost(new TargetControlledCreaturePermanent(1,1, filterOrc, true))));
    }

    public RaidingParty(final RaidingParty card) {
        super(card);
    }

    @Override
    public RaidingParty copy() {
        return new RaidingParty(this);
    }
}

class RaidingPartyEffect extends OneShotEffect {

    private static final FilterControlledCreaturePermanent filter = new FilterControlledCreaturePermanent("untapped white creatures");
    private static final FilterPermanent filter2 = new FilterPermanent("Plains");

    static {
        filter.add(Predicates.not(new TappedPredicate()));
        filter.add(new ColorPredicate(ObjectColor.WHITE));
        filter2.add(new SubtypePredicate(SubType.PLAINS));
    }

    RaidingPartyEffect() {
        super(Outcome.Detriment);
        staticText = "Each player may tap any number of untapped white creatures he or she controls. For each creature tapped this way, that player chooses up to two Plains. Then destroy all Plains that weren't chosen this way by any player";
    }

    RaidingPartyEffect(RaidingPartyEffect effect) {
        super(effect);
    }

    @Override
    public RaidingPartyEffect copy() {
        return new RaidingPartyEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent sourcePermanent = game.getPermanentOrLKIBattlefield(source.getSourceId());
        if (sourcePermanent != null) {
            Set<UUID> plainsToSave = new HashSet<>();
            for (UUID playerId : game.getState().getPlayersInRange(source.getControllerId(), game)) {
                Player player = game.getPlayer(playerId);
                if (player != null) {
                    int countBattlefield = game.getBattlefield().getAllActivePermanents(filter, game.getActivePlayerId(), game).size();
                    int tappedCount = 0;
                    Target untappedCreatureTarget = new TargetControlledCreaturePermanent(0, Integer.MAX_VALUE, filter, true);
                    if (player.choose(Outcome.Benefit, untappedCreatureTarget, source.getSourceId(), game)) {
                        tappedCount = untappedCreatureTarget.getTargets().size();
                        for (UUID creatureId : untappedCreatureTarget.getTargets()) {
                            Permanent creature = game.getPermanentOrLKIBattlefield(creatureId);
                            if (creature != null) {
                                creature.tap(game);
                            }
                        }
                    }
                    if (tappedCount > 0) {
                        Target plainsToSaveTarget = new TargetPermanent(0, tappedCount * 2, filter2, true);
                        if (player.choose(Outcome.Benefit, plainsToSaveTarget, source.getSourceId(), game)) {
                            for (UUID plainsId : plainsToSaveTarget.getTargets()) {
                                plainsToSave.add(plainsId);
                                Permanent plains = game.getPermanent(plainsId);
                                if (plains != null) {
                                    game.informPlayers(player.getLogName() + " chose " + plains.getLogName() + " to not be destroyed by " + sourcePermanent.getLogName());
                                }
                            }
                        }
                    }
                }
            }
            for (Permanent plains : game.getBattlefield().getActivePermanents(filter2, source.getControllerId(), source.getSourceId(), game)) {
                if (!plainsToSave.contains(plains.getId())) {
                    plains.destroy(source.getSourceId(), game, false);
                }
            }
            return true;
        }
        return false;
    }
}