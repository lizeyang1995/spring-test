package com.thoughtworks.rslist.service;

import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.domain.Vote;
import com.thoughtworks.rslist.dto.RsEventDto;
import com.thoughtworks.rslist.dto.TradeDto;
import com.thoughtworks.rslist.dto.UserDto;
import com.thoughtworks.rslist.dto.VoteDto;
import com.thoughtworks.rslist.repository.RsEventRepository;
import com.thoughtworks.rslist.repository.TradeRepository;
import com.thoughtworks.rslist.repository.UserRepository;
import com.thoughtworks.rslist.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.text.html.parser.Entity;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RsService {
  final RsEventRepository rsEventRepository;
  final UserRepository userRepository;
  final VoteRepository voteRepository;
  final TradeRepository tradeRepository;
  int[] priceOfRanking;

  public RsService(RsEventRepository rsEventRepository, UserRepository userRepository, VoteRepository voteRepository, TradeRepository tradeRepository) {
    this.rsEventRepository = rsEventRepository;
    this.userRepository = userRepository;
    this.voteRepository = voteRepository;
    this.tradeRepository = tradeRepository;
    this.priceOfRanking = new int[rsEventRepository.findAll().size()];
  }

  public void vote(Vote vote, int rsEventId) {
    Optional<RsEventDto> rsEventDto = rsEventRepository.findById(rsEventId);
    Optional<UserDto> userDto = userRepository.findById(vote.getUserId());
    if (!rsEventDto.isPresent()
        || !userDto.isPresent()
        || vote.getVoteNum() > userDto.get().getVoteNum()) {
      throw new RuntimeException();
    }
    VoteDto voteDto =
        VoteDto.builder()
            .localDateTime(vote.getTime())
            .num(vote.getVoteNum())
            .rsEvent(rsEventDto.get())
            .user(userDto.get())
            .build();
    voteRepository.save(voteDto);
    UserDto user = userDto.get();
    user.setVoteNum(user.getVoteNum() - vote.getVoteNum());
    userRepository.save(user);
    RsEventDto rsEvent = rsEventDto.get();
    rsEvent.setVoteNum(rsEvent.getVoteNum() + vote.getVoteNum());
    rsEventRepository.save(rsEvent);
  }

  public boolean buy(Trade trade, int rsEventId) {
    priceOfRanking = Arrays.copyOf(priceOfRanking, rsEventRepository.findAll().size());
    int wantedRank = trade.getRank();
    int purchaseAmount = trade.getAmount();
    Optional<RsEventDto> foundRsEvent = rsEventRepository.findById(rsEventId);
    if (!foundRsEvent.isPresent()) {
      throw new RuntimeException();
    }
    if (priceOfRanking.length >= wantedRank && priceOfRanking[wantedRank - 1] >= purchaseAmount) {
      return false;
    }
    RsEventDto rsEventDto = foundRsEvent.get();
    tradeRepository.save(TradeDto.builder().rank(trade.getRank()).rsEventDto(rsEventDto).amount(trade.getAmount()).build());
    priceOfRanking[trade.getRank() - 1] = trade.getAmount();
    rsEventRepository.deleteByRank(wantedRank);
    adjustRank();
    return true;
  }

  public void adjustRank() {
    int rank = 1;
    List<Integer> purchasedRank = new ArrayList<>();
    List<TradeDto> allTradeDtos = tradeRepository.findAll();
    List<Integer> purchasedRsEventId = new ArrayList<>();
    for (int i = allTradeDtos.size() - 1; i >= 0; i--) {
      RsEventDto rsEventDto = allTradeDtos.get(i).getRsEventDto();
      int newRank = allTradeDtos.get(i).getRank();
      rsEventDto.setRank(newRank);
      rsEventRepository.save(rsEventDto);
      purchasedRank.add(newRank);
      purchasedRsEventId.add(rsEventDto.getId());
    }
    List<RsEventDto> allRsEvents = rsEventRepository.findAll().stream().sorted((s1, s2) -> s2.getVoteNum() - s1.getVoteNum()).collect(Collectors.toList());
    for (int i = 0; i < allRsEvents.size(); i++) {
      RsEventDto rsEventDto = allRsEvents.get(i);
      if (purchasedRsEventId.contains(rsEventDto.getId())) {
        continue;
      }
      while (purchasedRank.contains(rank)) {
        rank++;
      }
      rsEventDto.setRank(rank);
      rank++;
    }
    rsEventRepository.saveAll(allRsEvents);
  }
}
