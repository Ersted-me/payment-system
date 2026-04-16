package com.ersted.walletservice.mapper;

import com.ersted.walletservice.entity.Wallet;
import com.ersted.walletservice.model.WalletResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mapping(target = "createdAt", source = "created")
    WalletResponse map(Wallet wallet);

    List<WalletResponse> map(List<Wallet> wallets);

}
