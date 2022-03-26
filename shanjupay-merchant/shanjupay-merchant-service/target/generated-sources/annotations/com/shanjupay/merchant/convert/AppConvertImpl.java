package com.shanjupay.merchant.convert;

import com.shanjupay.merchant.api.dto.AppDTO;
import com.shanjupay.merchant.entity.App;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2022-03-26T20:02:44+0800",
    comments = "version: 1.3.0.Final, compiler: javac, environment: Java 1.8.0_181 (Oracle Corporation)"
)
public class AppConvertImpl implements AppConvert {

    @Override
    public App dto2entity(AppDTO appDTO) {
        if ( appDTO == null ) {
            return null;
        }

        App app = new App();

        app.setAppId( appDTO.getAppId() );
        app.setAppName( appDTO.getAppName() );
        app.setMerchantId( appDTO.getMerchantId() );
        app.setPublicKey( appDTO.getPublicKey() );
        app.setNotifyUrl( appDTO.getNotifyUrl() );

        return app;
    }

    @Override
    public AppDTO entity2dto(App app) {
        if ( app == null ) {
            return null;
        }

        AppDTO appDTO = new AppDTO();

        appDTO.setAppId( app.getAppId() );
        appDTO.setAppName( app.getAppName() );
        appDTO.setMerchantId( app.getMerchantId() );
        appDTO.setPublicKey( app.getPublicKey() );
        appDTO.setNotifyUrl( app.getNotifyUrl() );

        return appDTO;
    }

    @Override
    public List<AppDTO> listentity2dto(List<App> appList) {
        if ( appList == null ) {
            return null;
        }

        List<AppDTO> list = new ArrayList<AppDTO>( appList.size() );
        for ( App app : appList ) {
            list.add( entity2dto( app ) );
        }

        return list;
    }
}
