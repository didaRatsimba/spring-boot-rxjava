package com.ojacquemart.reactive.github.service;

import com.ojacquemart.reactive.github.domain.GithubUser;
import com.ojacquemart.reactive.github.domain.RawUser;
import com.ojacquemart.reactive.github.domain.Repository;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Slf4j
@Service
public class GithubService {

    private RestClient restClient;

    public GithubService() {
        this(new RestClient());
    }

    public GithubService(RestClient restClient) {
        this.restClient = restClient;
    }

    public GithubUser getUser(String login) {
        Observable<RawUser> getRawUser = getRawUserObservable(login);
        Observable<RawUser[]> getFollowers = getFollowersObservable(login);
        Observable<Repository[]> getRepositories = getReposObservable(login);

        Observable<GithubUser> fullUser = Observable.zip(Arrays.asList(getRawUser, getFollowers, getRepositories), objects -> {
            RawUser rawUser = (RawUser) objects[0];
            RawUser[] followers = (RawUser[]) objects[1];
            Repository[] repositories = (Repository[]) objects[2];

            return new GithubUser(rawUser, Arrays.asList(followers), Arrays.asList(repositories));
        });

        return fullUser.blockingFirst();
    }

    private Observable<RawUser> getRawUserObservable(String login) {
        return Observable.<RawUser>create(s ->
                s.onNext(restClient.getUser(login))
        )
                .subscribeOn(Schedulers.computation())
                .onErrorReturn(throwable -> {
                    log.error("Failed to retrieve user {}", login, throwable);

                    return new RawUser("???", null, null);
                })
                ;
    }

    private Observable<RawUser[]> getFollowersObservable(String login) {
        return Observable.<RawUser[]>create(s ->
                s.onNext(restClient.getFollowers(login))
        )
                .onErrorReturn(throwable -> {
                    log.error("Failed to retrieve {} followers", login, throwable);

                    return new RawUser[]{};
                })
                .subscribeOn(Schedulers.computation())
                ;
    }

    private Observable<Repository[]> getReposObservable(String login) {
        return Observable.<Repository[]>create(s ->
                s.onNext(restClient.getRepositories(login))
        )
                .onErrorReturn(throwable -> {
                    log.error("Failed to retrieve {} repositories", login, throwable);

                    return new Repository[]{};
                })
                .subscribeOn(Schedulers.computation())
                ;
    }

}
